package com.commercetools.dataimport.products;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductsDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProductsDeleteJobConfiguration.class);

    @Bean
    public Job productsDeleteJob(Step unpublishProductsStep, Step deleteProductsStep) {
        return jobBuilderFactory.get("productsDeleteJob")
                .start(unpublishProductsStep)
                .next(deleteProductsStep)
                .build();
    }

    @Bean
    protected Step unpublishProductsStep(final ItemReader<Product> unpublishReader, final ItemWriter<Product> unpublishWriter) {
        return stepBuilderFactory.get("unpublishProductsStep")
                .<Product, Product>chunk(50)
                .reader(unpublishReader)
                .writer(unpublishWriter)
                .build();
    }

    @Bean
    protected Step deleteProductsStep(final ItemReader<Product> productReader, final ItemWriter<Product> productWriter) {
        return stepBuilderFactory.get("deleteProductsStep")
                .<Product, Product>chunk(50)
                .reader(productReader)
                .writer(productWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<Product> unpublishReader(final BlockingSphereClient sphereClient) {
        final ProductQuery query = ProductQuery.of().withPredicates(m -> m.masterData().isPublished().is(true));
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, query);
    }

    @Bean
    public ItemWriter<Product> unpublishWriter(final BlockingSphereClient sphereClient) {
        return items -> items.stream()
                .peek(element -> LOGGER.debug("Attempting to delete product " + element.getId()))
                .map(product -> ProductUpdateCommand.of(product, Unpublish.of()))
                .forEach(sphereClient::executeBlocking);
    }

    @Bean
    public ItemStreamReader<Product> productReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of());
    }

    @Bean
    public ItemWriter<Product> productWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.execute(ProductDeleteCommand.of(item)));
    }
}
