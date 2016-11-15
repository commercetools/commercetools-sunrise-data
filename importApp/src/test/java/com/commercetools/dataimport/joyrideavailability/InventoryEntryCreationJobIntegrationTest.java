package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.withInventoryEntry;
import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.withProduct;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CommercetoolsTestConfiguration.class})
@EnableAutoConfiguration
@Configuration
@TestPropertySource("classpath:/test.properties")
public class InventoryEntryCreationJobIntegrationTest extends JoyrideAvailabilityIntegrationTest {
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
}