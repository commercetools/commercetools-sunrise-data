package com.commercetools.dataimport;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductsDeleteStepConfiguration {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProductsDeleteStepConfiguration.class);

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    @JobScope
    public Step productsUnpublishStep() {
        return stepBuilderFactory.get("unpublishProductsStep")
                .<Product, Product>chunk(50)
                .reader(productsUnpublishStepReader())
                .writer(productsUnpublishStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step productsDeleteStep() {
        return stepBuilderFactory.get("productsDeleteStep")
                .<Product, Product>chunk(50)
                .reader(productsDeleteStepReader())
                .writer(productsDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step productTypeDeleteStep() {
        return stepBuilderFactory.get("productTypeDeleteStep")
                .<ProductType, ProductType>chunk(1)
                .reader(productTypeDeleteStepReader())
                .writer(productTypeDeleteStepWriter())
                .build();
    }

    private ItemStreamReader<Product> productsUnpublishStepReader() {
        final ProductQuery query = ProductQuery.of().withPredicates(m -> m.masterData().isPublished().is(true));
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, query);
    }

    private ItemWriter<Product> productsUnpublishStepWriter() {
        return items -> items.stream()
                .peek(element -> LOGGER.debug("Attempting to delete product " + element.getId()))
                .map(product -> ProductUpdateCommand.of(product, Unpublish.of()))
                .forEach(sphereClient::executeBlocking);
    }

    private ItemStreamReader<Product> productsDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of());
    }

    private ItemWriter<Product> productsDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final Product product = sphereClient.executeBlocking(ProductDeleteCommand.of(item));
            LOGGER.debug("Removed product \"{}\"", product.getId());
        });
    }

    private ItemStreamReader<ProductType> productTypeDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductTypeQuery.of());
    }

    private ItemWriter<ProductType> productTypeDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final ProductType productType = sphereClient.executeBlocking(ProductTypeDeleteCommand.of(item));
            LOGGER.debug("Removed product type \"{}\"", productType.getName());
        });
    }
}
