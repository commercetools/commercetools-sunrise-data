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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;

import static com.commercetools.dataimport.joyrideavailability.InventoryEntryCreationJobConfiguration.createInventoryEntryDraftForProductVariant;
import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, InventoryEntryCreationJobConfiguration.class, CommercetoolsTestConfiguration.class})
@EnableAutoConfiguration
@Configuration
@TestPropertySource("classpath:/test.properties")
public class InventoryEntryCreationJobIntegrationTest extends JoyrideAvailabilityIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("test")
    private BlockingSphereClient sphereClient;

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
        final int amountOfProducts = 3;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productProjections -> {
                assertThat(productProjections.size()).isEqualTo(amountOfProducts);
                final InventoryEntryQuery inventoryBaseQuery = InventoryEntryQuery.of();
                final PagedQueryResult<InventoryEntry> inventoryEntryPagedQueryResult = sphereClient.executeBlocking(inventoryBaseQuery.withLimit(0));
                assertThat(inventoryEntryPagedQueryResult.getTotal()).isZero();
                executeInventoryEntryCreationJob();
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
                return productProjections;
            });
        });
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

    private void executeInventoryEntryCreationJob() {
        try {
            final JobExecution jobExecution = jobLauncherTestUtils.launchJob();
            assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}