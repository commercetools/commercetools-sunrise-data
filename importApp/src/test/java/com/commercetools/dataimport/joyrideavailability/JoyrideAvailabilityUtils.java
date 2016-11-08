package com.commercetools.dataimport.joyrideavailability;

import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.LocalizedString;
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
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class JoyrideAvailabilityUtils {

    public static ProductType createProductType(final BlockingSphereClient sphereClient) {
        final ProductTypeDraft productTypeDraft =
                ProductTypeDraft.of(RandomStringUtils.randomAlphabetic(10), "name", "a 'T' shaped cloth", Collections.emptyList());
        final ProductType productType = sphereClient.executeBlocking(ProductTypeCreateCommand.of(productTypeDraft));
        return productType;
    }

    public static Product createProduct(final BlockingSphereClient sphereClient, final ProductType productType, final String sku) {
        final ProductDraftBuilder productDraftBuilder = ProductDraftBuilder.of(productType, LocalizedString.of(Locale.ENGLISH, "product-name"),
                LocalizedString.of(Locale.ENGLISH, RandomStringUtils.randomAlphabetic(10)), ProductVariantDraftBuilder.of().sku(sku).build());
        final Product product = sphereClient.executeBlocking(ProductCreateCommand.of(productDraftBuilder.build()));
        final Product publishedProduct = sphereClient.executeBlocking(ProductUpdateCommand.of(product, Publish.of()));
        assertThat(publishedProduct.getMasterData().isPublished()).isTrue();
        return publishedProduct;
    }

    public static void deleteInventoryEntries(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, InventoryEntryQuery.of(), (inventoryEntry) -> sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(inventoryEntry)));
    }

    public static void deleteChannels(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ChannelQuery.of(), (channel) -> sphereClient.executeBlocking(ChannelDeleteCommand.of(channel)));
    }

    public static void unpublishProducts(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ProductProjectionQuery.ofCurrent(), (item) -> {
            final ProductUpdateCommand of = ProductUpdateCommand.of(item, Unpublish.of());
            return sphereClient.executeBlocking(of);
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
