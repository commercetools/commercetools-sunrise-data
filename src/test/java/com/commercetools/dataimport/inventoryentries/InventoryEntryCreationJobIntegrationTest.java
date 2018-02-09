package com.commercetools.dataimport.inventoryentries;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.IntegrationTest;
import com.commercetools.dataimport.TestConfiguration;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.dataimport.TestUtils.*;
import static com.commercetools.dataimport.channels.PreferredChannels.CHANNEL_KEYS;
import static com.commercetools.dataimport.inventoryentries.InventoryEntryCreationJobConfiguration.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, InventoryEntryCreationJobConfiguration.class, CommercetoolsTestConfiguration.class})
@EnableAutoConfiguration
@Configuration
@TestPropertySource("classpath:/test.properties")
@NotThreadSafe
public class InventoryEntryCreationJobIntegrationTest extends IntegrationTest {

    @Autowired
    private Environment env;

    @Autowired
    @Qualifier("test")
    private BlockingSphereClient sphereClient;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private ItemProcessor<ProductProjection, List<InventoryEntryDraft>> inventoryEntryProcessor;

    @Autowired
    private ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter;

    @Test
    public void findLastProductWithInventoryEntries() throws Exception {
        final int amountOfProducts = 10;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productsWithoutInventories -> {
                assertThat(productsWithoutInventories).hasSize(amountOfProducts);
                final Long lastProductIndex = (long) (amountOfProducts - 1);
                final Optional<ProductProjection> lastProductWithInventory = findLastProductWithInventory(sphereClient);
                assertThat(lastProductWithInventory).isEmpty();

                final int stopIndex = 6;
                addInventoriesToProductsUntilStopIndex(stopIndex);
                final Optional<ProductProjection> lastProductWithInventory1 = findLastProductWithInventory(sphereClient);
                final ProductProjection stopProduct = productsWithoutInventories.get(stopIndex);
                assertThat(stopProduct.getId()).isEqualTo(lastProductWithInventory1.get().getId());

                addInventoriesToMissingProducts();
                final Optional<ProductProjection> lastProductWithInventory2 = findLastProductWithInventory(sphereClient);
                final ProductProjection lastProduct = productsWithoutInventories.get(lastProductIndex.intValue());
                assertThat(lastProduct.getId()).isEqualTo(lastProductWithInventory2.get().getId());

                deleteInventoryEntries(sphereClient);
                return sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent()).getResults();
            });
        });
    }

    @Test
    public void jobInventoryEntryCreation() throws Exception {
        final int amountOfProducts = 10;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productProjections -> {
                assertThat(productProjections).hasSize(amountOfProducts);
                assertThat(joyrideChannels).hasSize(CHANNEL_KEYS.size());
                final InventoryEntryQuery inventoryBaseQuery = InventoryEntryQuery.of();
                final PagedQueryResult<InventoryEntry> inventoryEntryPagedQueryResult = sphereClient.executeBlocking(inventoryBaseQuery.withLimit(0));
                assertThat(inventoryEntryPagedQueryResult.getTotal()).isZero();
                executeInventoryEntryCreationJob();
                validateInventoryEntries(amountOfProducts, joyrideChannels, productProjections, inventoryBaseQuery);
                deleteInventoryEntries(sphereClient);
                return productProjections;
            });
        });
    }

    @Test
    public void readerRestartAfterPartialExecution() throws Exception {
        final int amountOfProducts = 10;
        final int stopIndex = 6;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productsWithoutInventories -> {
                addInventoriesToProductsUntilStopIndex(stopIndex);
                final ItemReader<ProductProjection> restartReader = createProductProjectionReader(sphereClient);
                final ProductProjection restartProduct = restartReader.read();
                assertThat(restartProduct.getId()).isEqualTo(productsWithoutInventories.get(stopIndex + 1).getId());
                deleteInventoryEntries(sphereClient);
                return productsWithoutInventories;
            });
        });
    }

    private void validateInventoryEntries(final int amountOfProducts, final List<Channel> joyrideChannels, final List<ProductProjection> productProjections, final InventoryEntryQuery inventoryBaseQuery) {
        final PagedQueryResult<InventoryEntry> inventoryEntriesAfterJobExecution = sphereClient.executeBlocking(inventoryBaseQuery);
        final int expectedAmountOfInventoryEntries = joyrideChannels.size() * amountOfProducts;
        assertThat(inventoryEntriesAfterJobExecution.getTotal()).isEqualTo(expectedAmountOfInventoryEntries);
        inventoryEntriesAfterJobExecution
                .getResults()
                .forEach(inventoryEntry -> {
                    final Long expectedStockQuantity = stockQuantityByChannelAndProductVariant(joyrideChannels, productProjections, inventoryEntry);
                    assertThat(expectedStockQuantity).isEqualTo(inventoryEntry.getQuantityOnStock());
                });
    }

    private void addInventoriesToProductsUntilStopIndex(final int stopIndex) throws Exception {
        final ItemReader<ProductProjection> initialReader = InventoryEntryCreationJobConfiguration.createProductProjectionReader(sphereClient);
        int index = 0;
        while (index <= stopIndex) {
            final List<InventoryEntryDraft> list = inventoryEntryProcessor.process(initialReader.read());
            inventoryEntryWriter.write(asList(list));
            index++;
        }
    }

    private void addInventoriesToMissingProducts() throws Exception {
        final ItemReader<ProductProjection> restartReader = InventoryEntryCreationJobConfiguration.createProductProjectionReader(sphereClient);
        ProductProjection product;
        while ((product = restartReader.read()) != null) {
            final List<InventoryEntryDraft> list = inventoryEntryProcessor.process(product);
            inventoryEntryWriter.write(asList(list));
        }
    }

    private Long stockQuantityByChannelAndProductVariant(final List<Channel> joyrideChannels, final List<ProductProjection> productProjections, final InventoryEntry inventoryEntry) {
        final ProductVariant masterVariant = productProjections.stream()
                .filter(m -> m.getMasterVariant().getSku().equals(inventoryEntry.getSku()))
                .findFirst()
                .get()
                .getMasterVariant();
        final Channel inventoryEntryChannel = joyrideChannels.stream()
                .filter(m -> m.getId().equals(inventoryEntry.getSupplyChannel().getId()))
                .findFirst()
                .get();
        final InventoryEntryDraft entryDraftForProductVariant = createInventoryEntryDraftForProductVariant(inventoryEntryChannel, masterVariant);
        return entryDraftForProductVariant.getQuantityOnStock();
    }

    private void executeInventoryEntryCreationJob() throws Exception {
        final Map<String, JobParameter> jobParametersMap = new HashMap<>();
        addCommercetoolsCredentialValues(env, jobParametersMap);
        final JobParameters jobParameters = new JobParameters(jobParametersMap);
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
}