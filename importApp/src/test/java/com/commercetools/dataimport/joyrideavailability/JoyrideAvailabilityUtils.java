package com.commercetools.dataimport.joyrideavailability;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.queries.PagedQueryResult;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class JoyrideAvailabilityUtils {

    public static void deleteInventoryEntries(final BlockingSphereClient sphereClient) {
        int deletedInventoriesAmount;
        do {
            final PagedQueryResult<InventoryEntry> inventoryEntries = sphereClient.executeBlocking(InventoryEntryQuery.of());
            final List<InventoryEntry> deletedInventoryEntries = inventoryEntries.getResults()
                    .stream()
                    .peek(element -> System.err.println("attempting to delete inventory entry " + element.getId()))
                    .map(item -> sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(item)))
                    .collect(toList());
            deletedInventoriesAmount = deletedInventoryEntries.size();
        } while (deletedInventoriesAmount > 0);
    }

    public static void deleteChannels(final BlockingSphereClient sphereClient) {
        int deletedChannelsAmount;
        do {
            final PagedQueryResult<Channel> channels = sphereClient.executeBlocking(ChannelQuery.of());
            final List<Channel> deletedChannels = channels.getResults()
                    .stream()
                    .peek(element -> System.err.println("attempting to delete channel " + element.getId()))
                    .map(item -> sphereClient.executeBlocking(ChannelDeleteCommand.of(item)))
                    .collect(toList());
            deletedChannelsAmount = deletedChannels.size();
        } while (deletedChannelsAmount > 0);
    }

    public static void unpublishProducts(final BlockingSphereClient sphereClient) {
        int updatedProductsAmount;
        do {
            final PagedQueryResult<ProductProjection> products = sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent());
            final List<Product> updatedProducts = products.getResults()
                    .stream()
                    .peek(element -> System.err.println("attempting to unpublish product " + element.getId()))
                    .map(item -> sphereClient.executeBlocking(ProductUpdateCommand.of(item, Unpublish.of())))
                    .collect(toList());
            updatedProductsAmount = updatedProducts.size();
        } while (updatedProductsAmount > 0);
    }

    public static void deleteProducts(final BlockingSphereClient sphereClient) {
        int deletedProductsAmount;
        do {
            final PagedQueryResult<ProductProjection> products = sphereClient.executeBlocking(ProductProjectionQuery.ofStaged());
            final List<Product> deletedProducts = products.getResults()
                    .stream()
                    .peek(element -> System.err.println("attempting to delete product " + element.getId()))
                    .map(item -> sphereClient.executeBlocking(ProductDeleteCommand.of(item)))
                    .collect(toList());
            deletedProductsAmount = deletedProducts.size();
        } while (deletedProductsAmount > 0);
    }
}
