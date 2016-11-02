package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.categories.TestConfiguration;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@ContextConfiguration(classes = {TestConfiguration.class, AvailabilityPricesImportJobConfiguration.class, CommercetoolsTestConfiguration.class})
@TestPropertySource("/test.properties")
@EnableAutoConfiguration
@Configuration
public class AvailabilityPricesImportJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("test")
    private BlockingSphereClient sphereClient;

    @Test
    public void findLastModifiedProductTest() throws Exception {
        final String sku = RandomStringUtils.randomAlphabetic(10);
        createProduct(createProductType(), sku);
        final Optional<ProductProjection> lastModifiedProduct = AvailabilityPricesImportJobConfiguration.findLastModifiedProduct(sphereClient);
        assertThat(lastModifiedProduct).isPresent();
        assertThat(lastModifiedProduct.get().getAllVariants().get(0).getSku()).isEqualTo(sku);
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
        final Product publishedProduct = sphereClient.executeBlocking(ProductUpdateCommand.of(product, Publish.of()));
        assertThat(publishedProduct.getMasterData().isPublished()).isTrue();
        return publishedProduct;
    }

}
