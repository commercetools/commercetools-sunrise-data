package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

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
        final String sku = RandomStringUtils.randomAlphabetic(10);
        createInventoryEntry(sku);
        final Optional<InventoryEntry> lastInventoryEntry = InventoryEntryCreationJobConfiguration.findLastInventoryEntry(sphereClient);
        assertThat(lastInventoryEntry).isPresent();
        assertThat(lastInventoryEntry.get().getSku()).isEqualTo(sku);
    }

    private ProductType createProductType() {
        final ProductTypeDraft productTypeDraft =
                ProductTypeDraft.of(RandomStringUtils.randomAlphabetic(10), "name", "a 'T' shaped cloth", Collections.emptyList());
        final ProductType productType = sphereClient.executeBlocking(ProductTypeCreateCommand.of(productTypeDraft));
        return productType;
    }

    private Product createProduct(final ProductType productType, final String sku) {
        final ProductDraftBuilder productDraftBuilder = ProductDraftBuilder.of(productType, LocalizedString.of(Locale.ENGLISH, "prodcut-name"),
                LocalizedString.of(Locale.ENGLISH, RandomStringUtils.randomAlphabetic(10)), ProductVariantDraftBuilder.of().sku(sku).build());
        final Product product = sphereClient.executeBlocking(ProductCreateCommand.of(productDraftBuilder.build()));
        return product;
    }

    private void createInventoryEntry(final String sku) {
        createProduct(createProductType(), sku);
        final Long quantityOnStock = 50L;
        final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraft.of(sku, quantityOnStock);
        sphereClient.executeBlocking(InventoryEntryCreateCommand.of(inventoryEntryDraft));
    }
}