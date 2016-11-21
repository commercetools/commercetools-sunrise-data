package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.categories.TestConfiguration;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
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

import static com.commercetools.dataimport.joyrideavailability.InventoryEntryCreationJobConfiguration.createInventoryEntryDraftForProductVariant;
import static com.commercetools.dataimport.joyrideavailability.InventoryEntryCreationJobConfiguration.createReader;
import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, InventoryEntryCreationJobConfiguration.class, CommercetoolsTestConfiguration.class})
@EnableAutoConfiguration
@Configuration
@TestPropertySource("classpath:/test.properties")
@NotThreadSafe
public class InventoryEntryCreationJobIntegrationTest extends JoyrideAvailabilityIntegrationTest {

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
    public void findLastInventoryEntryTest() throws Exception {
        withProduct(sphereClient, product -> {
            final String sku = product.getMasterData().getCurrent().getMasterVariant().getSku();
            withInventoryEntry(sphereClient, sku, inventoryEntry -> {
                final Optional<InventoryEntry> lastInventoryEntry = InventoryEntryCreationJobConfiguration.findLastInventoryEntry(sphereClient);
                assertThat(lastInventoryEntry).isPresent();
                assertThat(lastInventoryEntry.get()).isEqualTo(inventoryEntry);
            });
            return product;
        });
    }

    @Test
    public void jobInventoryEntryCreation() throws Exception {
        final int amountOfProducts = 4;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productProjections -> {
                assertThat(productProjections).hasSize(amountOfProducts);
                assertThat(joyrideChannels).hasSize(PreferredChannels.CHANNEL_KEYS.size());
                final InventoryEntryQuery inventoryBaseQuery = InventoryEntryQuery.of();
                final PagedQueryResult<InventoryEntry> inventoryEntryPagedQueryResult = sphereClient.executeBlocking(inventoryBaseQuery.withLimit(0));
                assertThat(inventoryEntryPagedQueryResult.getTotal()).isZero();
                executeInventoryEntryCreationJob();
                validateInventoryEntries(amountOfProducts, joyrideChannels, productProjections, inventoryBaseQuery);
                return productProjections;
            });
        });
    }

    @Test
    public void restartAfterPartialExecution() throws Exception {
        final int amountOfProducts = 10;
        final int stopIndex = 6;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productProjections -> {
                assertThat(productProjections).hasSize(amountOfProducts);
                final InventoryEntryQuery inventoryBaseQuery = InventoryEntryQuery.of();
                final PagedQueryResult<InventoryEntry> inventoryEntryPagedQueryResult = sphereClient.executeBlocking(inventoryBaseQuery.withLimit(0));
                assertThat(inventoryEntryPagedQueryResult.getTotal()).isZero();
                executePartiallyInventoryCreationJob(stopIndex);
                final ItemReader<ProductProjection> restartReader = createReader(sphereClient);
                final ProductProjection afterRestartProductRead = restartReader.read();
                assertThat(afterRestartProductRead.getId()).isEqualTo(productProjections.get(stopIndex).getId());

                endExecutionInventoryCreationJob(restartReader);
                validateInventoryEntries(amountOfProducts, joyrideChannels, productProjections, inventoryBaseQuery);
                return productProjections;
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
        deleteInventoryEntries(sphereClient);
    }

    private void endExecutionInventoryCreationJob(final ItemReader<ProductProjection> reader) throws Exception {
        ProductProjection product;
        while ((product = reader.read()) != null) {
            final List<InventoryEntryDraft> list = inventoryEntryProcessor.process(product);
            inventoryEntryWriter.write(asList(list));
        }
    }

    private void executePartiallyInventoryCreationJob(final int stopIndex) throws Exception {
        final ItemReader<ProductProjection> initialReader = createReader(sphereClient);
        for (int i = 0; i <= stopIndex; i++) {
            final ProductProjection read = initialReader.read();
            final List<InventoryEntryDraft> list = inventoryEntryProcessor.process(read);
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