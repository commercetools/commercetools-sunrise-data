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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductsDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProductsDeleteJobConfiguration.class);

    @Bean
    public Job productsDeleteJob(final Step productsUnpublishStep, final Step productsDeleteStep) {
        return jobBuilderFactory.get("productsDeleteJob")
                .start(productsUnpublishStep)
                .next(productsDeleteStep)
                .build();
    }

    @Bean
    protected Step productsUnpublishStep(final ItemReader<Product> productsUnpublishReader,
                                         final ItemWriter<Product> productsUnpublishWriter) {
        return stepBuilderFactory.get("unpublishProductsStep")
                .<Product, Product>chunk(50)
                .reader(productsUnpublishReader)
                .writer(productsUnpublishWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<Product> productsUnpublishReader(final BlockingSphereClient sphereClient) {
        final ProductQuery query = ProductQuery.of().withPredicates(m -> m.masterData().isPublished().is(true));
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, query);
    }

    @Bean
    public ItemWriter<Product> productsUnpublishWriter(final BlockingSphereClient sphereClient) {
        return items -> items.stream()
                .peek(element -> LOGGER.debug("Attempting to delete product " + element.getId()))
                .map(product -> ProductUpdateCommand.of(product, Unpublish.of()))
                .forEach(sphereClient::executeBlocking);
    }

    @Bean
    protected Step productsDeleteStep(final ItemReader<Product> productsDeleteReader,
                                      final ItemWriter<Product> productsDeleteWriter) {
        return stepBuilderFactory.get("productsDeleteStep")
                .<Product, Product>chunk(50)
                .reader(productsDeleteReader)
                .writer(productsDeleteWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<Product> productsDeleteReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of());
    }

    @Bean
    public ItemWriter<Product> productsDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.execute(ProductDeleteCommand.of(item)));
    }
}
