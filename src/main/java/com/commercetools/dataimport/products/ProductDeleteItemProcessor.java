package com.commercetools.dataimport.products;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import org.springframework.batch.item.ItemProcessor;

public class ProductDeleteItemProcessor implements ItemProcessor<ProductProjection, ProductDeleteCommand> {

    private BlockingSphereClient sphereClient;

    public ProductDeleteItemProcessor(final BlockingSphereClient sphereClient) {
        this.sphereClient = sphereClient;
    }

    @Override
    public ProductDeleteCommand process(final ProductProjection product) {
        if (product.isPublished()) {
            final ProductUpdateCommand unpublishCommand = ProductUpdateCommand.of(product, Unpublish.of());
            final Product unpublishedProduct = sphereClient.executeBlocking(unpublishCommand);
            return ProductDeleteCommand.of(unpublishedProduct);
        } else {
            return ProductDeleteCommand.of(product);
        }
    }
}
