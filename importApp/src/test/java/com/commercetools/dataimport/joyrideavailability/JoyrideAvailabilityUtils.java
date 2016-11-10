package com.commercetools.dataimport.joyrideavailability;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.Query;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.dataimport.joyrideavailability.PreferredChannels.CHANNEL_KEYS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class JoyrideAvailabilityUtils {

    public static void withPriceWithJoyrideChannel(final BlockingSphereClient sphereClient, final Consumer<PriceDraft> consumer) {
        final String joyrideChannelKey = CHANNEL_KEYS.get(0);
        final Channel joyrideChannel = sphereClient.executeBlocking(ChannelCreateCommand.of(ChannelDraft.of(joyrideChannelKey)));
        final PriceDraft priceDraft = PriceDraft.of(MoneyImpl.of(new BigDecimal("123456"), "EUR")).withChannel(joyrideChannel);
        consumer.accept(priceDraft);
        sphereClient.executeBlocking(ChannelDeleteCommand.of(joyrideChannel));
    }

    public static ProductType createProductType(final BlockingSphereClient sphereClient) {
        final ProductTypeDraft productTypeDraft =
                ProductTypeDraft.of(RandomStringUtils.randomAlphabetic(10), "name", "a 'T' shaped cloth", Collections.emptyList());
        final ProductType productType = sphereClient.executeBlocking(ProductTypeCreateCommand.of(productTypeDraft));
        return productType;
    }

    public static Product createProduct(final BlockingSphereClient sphereClient) {
        final String sku = RandomStringUtils.randomAlphabetic(10);
        final ProductType productType = createProductType(sphereClient);
        final ProductDraftBuilder productDraftBuilder = ProductDraftBuilder.of(productType, LocalizedString.of(Locale.ENGLISH, "product-name"),
                LocalizedString.of(Locale.ENGLISH, RandomStringUtils.randomAlphabetic(10)), ProductVariantDraftBuilder.of().sku(sku).build());
        final Product product = sphereClient.executeBlocking(ProductCreateCommand.of(productDraftBuilder.build()));
        final Product publishedProduct = sphereClient.executeBlocking(ProductUpdateCommand.of(product, Publish.of()));
        assertThat(publishedProduct.getMasterData().isPublished()).isTrue();
        return publishedProduct;
    }

    public static void createProducts(final BlockingSphereClient sphereClient, final int productsAmount) {
        for (int i = 0; i < productsAmount; i++) {
            createProduct(sphereClient);
        }
    }

    public static void deleteInventoryEntries(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, InventoryEntryQuery.of(), (inventoryEntry) -> sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(inventoryEntry)));
    }

    public static void deleteChannels(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ChannelQuery.of(), (channel) -> sphereClient.executeBlocking(ChannelDeleteCommand.of(channel)));
    }

    public static void unpublishProducts(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ProductProjectionQuery.ofCurrent(), (item) -> {
            final ProductUpdateCommand updateCommand = ProductUpdateCommand.of(item, Unpublish.of());
            return sphereClient.executeBlocking(updateCommand);
        });
    }

    public static void deleteProducts(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ProductProjectionQuery.ofStaged(), (item) -> sphereClient.executeBlocking(ProductDeleteCommand.of(item)));
    }

    public static <T, S> void updateOrDeleteResources(final BlockingSphereClient sphereClient, final Query<T> query, final Function<T, S> function) {
        int modifiedResourcesAmount;
        do {
            final PagedQueryResult<T> pagedQueryResult = sphereClient.executeBlocking(query);
            final List<S> modifiedResources = pagedQueryResult.getResults()
                    .stream()
                    .map(item -> function.apply(item))
                    .collect(toList());
            modifiedResourcesAmount = modifiedResources.size();
        } while (modifiedResourcesAmount > 0);
    }
}
