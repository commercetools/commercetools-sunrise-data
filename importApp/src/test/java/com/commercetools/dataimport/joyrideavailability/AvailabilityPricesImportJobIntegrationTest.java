package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.categories.TestConfiguration;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@ContextConfiguration(classes = {TestConfiguration.class, AvailabilityPricesImportJobConfiguration.class, CommercetoolsTestConfiguration.class})
@TestPropertySource("/test.properties")
@EnableAutoConfiguration
@Configuration
public class AvailabilityPricesImportJobIntegrationTest extends JoyrideAvailabilityIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void findLastProductWithJoyridePrice() {

        final String sku = RandomStringUtils.randomAlphabetic(10);
        final Product product = createProduct(createProductType(), sku);
        final String joyrideChannelKey = PreferredChannels.CHANNEL_KEYS.get(0);
        final Channel joyrideChannel = sphereClient.executeBlocking(ChannelCreateCommand.of(ChannelDraft.of(joyrideChannelKey)));
        final PriceDraft priceDraft = PriceDraft.of(MoneyImpl.of(new BigDecimal("123456"), "EUR")).withChannel(joyrideChannel);
        final AddPrice addPrice = AddPrice.of(product.getMasterData().getCurrent().getMasterVariant().getId(), priceDraft);
        Product updatedProduct = sphereClient.executeBlocking(ProductUpdateCommand.of(product, asList(addPrice, Publish.of())));

        final ProductProjectionQuery productProjectionQuery1 = ProductProjectionQuery.ofCurrent().withSort(m -> m.id().sort().asc());
        final Long productsCount = sphereClient.executeBlocking(productProjectionQuery1).getTotal();

        final Optional<ProductProjection> lastProductWithJoyrideChannel = AvailabilityPricesImportJobConfiguration.findLastProductWithJoyrideChannel(sphereClient, 0L, productsCount - 1);
        assertThat(lastProductWithJoyrideChannel).isPresent();
        assertThat(lastProductWithJoyrideChannel.get().getAllVariants().get(0).getSku()).isEqualTo(sku);

        final Product productToDelete = sphereClient.executeBlocking(ProductUpdateCommand.of(updatedProduct, Unpublish.of()));
        sphereClient.executeBlocking(ProductDeleteCommand.of(productToDelete));
        sphereClient.executeBlocking(ChannelDeleteCommand.of(joyrideChannel));
    }

    private ProductType createProductType() {
        final ProductTypeDraft productTypeDraft =
                ProductTypeDraft.of(RandomStringUtils.randomAlphabetic(10), "name", "a 'T' shaped cloth", Collections.emptyList());
        final ProductType productType = sphereClient.executeBlocking(ProductTypeCreateCommand.of(productTypeDraft));
        return productType;
    }

    private Product createProduct(final ProductType productType, final String sku) {
        final ProductDraftBuilder productDraftBuilder = ProductDraftBuilder.of(productType, LocalizedString.of(Locale.ENGLISH, "product-name"),
                LocalizedString.of(Locale.ENGLISH, RandomStringUtils.randomAlphabetic(10)), ProductVariantDraftBuilder.of().sku(sku).build());
        final Product product = sphereClient.executeBlocking(ProductCreateCommand.of(productDraftBuilder.build()));
        final Product publishedProduct = sphereClient.executeBlocking(ProductUpdateCommand.of(product, Publish.of()));
        assertThat(publishedProduct.getMasterData().isPublished()).isTrue();
        return publishedProduct;
    }

}