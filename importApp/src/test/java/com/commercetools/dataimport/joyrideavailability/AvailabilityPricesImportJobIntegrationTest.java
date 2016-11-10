package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.categories.TestConfiguration;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.commercetools.dataimport.joyrideavailability.AvailabilityPricesImportJobConfiguration.*;
import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.*;
import static com.commercetools.dataimport.joyrideavailability.PreferredChannels.CHANNEL_KEYS;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
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
        final Product product = createProduct(sphereClient);
        final Long productsCount = 1L;
        assertThat(findLastProductWithJoyrideChannel(sphereClient, 0L, productsCount - 1)).isEmpty();

        withPriceWithJoyrideChannel(sphereClient, priceDraft -> {
            final AddPrice addPrice = AddPrice.of(product.getMasterData().getCurrent().getMasterVariant().getId(), priceDraft);
            Product updatedProduct = sphereClient.executeBlocking(ProductUpdateCommand.of(product, asList(addPrice, Publish.of())));
            final Optional<ProductProjection> lastProductWithJoyrideChannel = findLastProductWithJoyrideChannel(sphereClient, 0L, productsCount - 1);
            assertThat(lastProductWithJoyrideChannel).isPresent();
            assertThat(lastProductWithJoyrideChannel.get().getMasterVariant().getSku()).isEqualTo(product.getMasterData().getCurrent().getMasterVariant().getSku());
            final Product productToDelete = sphereClient.executeBlocking(ProductUpdateCommand.of(updatedProduct, Unpublish.of()));
            sphereClient.executeBlocking(ProductDeleteCommand.of(productToDelete));
        });
    }

    @Test
    public void readerRestartAfterLastProductWithJoyride() throws Exception {
        final int amountOfProducts = 10;
        createProducts(sphereClient, amountOfProducts);
        final ItemReader<ProductProjection> initialReader = createReader(sphereClient);
        ProductProjection productRead;
        List<ProductProjection> nonProcessedProducts = new ArrayList<>();
        while ((productRead = initialReader.read()) != null) {
            assertThat(productContainJoyridePrice(productRead)).isFalse();
            nonProcessedProducts.add(productRead);
        }
        final String joyrideChannelKey = CHANNEL_KEYS.get(0);
        final Channel joyrideChannel = sphereClient.executeBlocking(ChannelCreateCommand.of(ChannelDraft.of(joyrideChannelKey)));
        final PriceDraft priceDraft = PriceDraft.of(MoneyImpl.of(new BigDecimal("123456"), "EUR")).withChannel(joyrideChannel);
        final int restartIndex = amountOfProducts / 2;
        final List<Product> updatedProducts = nonProcessedProducts
                .stream()
                .limit(restartIndex)
                .peek(product -> System.err.println("attempting to add price to product " + product.getId()))
                .map(product -> {
                    final AddPrice addPrice = AddPrice.of(product.getMasterVariant().getId(), priceDraft);
                    return sphereClient.executeBlocking(ProductUpdateCommand.of(product, asList(addPrice, Publish.of())));
                })
                .collect(toList());
        final ItemReader<ProductProjection> afterLastModifiedProductReader = createReader(sphereClient);
        ProductProjection restartProduct = afterLastModifiedProductReader.read();
        assertThat(restartProduct.getId()).isEqualTo(nonProcessedProducts.get(restartIndex).getId());

    }
}