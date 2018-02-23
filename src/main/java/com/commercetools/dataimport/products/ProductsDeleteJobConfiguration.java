package com.commercetools.dataimport.products;

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
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class ProductsDeleteJobConfiguration {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProductsDeleteJobConfiguration.class);

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    public Job productsDeleteJob() {
        return jobBuilderFactory.get("productsDeleteJob")
                .start(productsUnpublishStep())
                .next(productsDeleteStep())
                .build();
    }

    @Bean
    @JobScope
    public Step productsUnpublishStep() {
        return stepBuilderFactory.get("unpublishProductsStep")
                .<Product, Product>chunk(50)
                .reader(unpublishReader())
                .writer(unpublishWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step productsDeleteStep() {
        return stepBuilderFactory.get("productsDeleteStep")
                .<Product, Product>chunk(50)
                .reader(deleteReader())
                .writer(deleteWriter())
                .build();
    }

    private ItemStreamReader<Product> unpublishReader() {
        final ProductQuery query = ProductQuery.of().withPredicates(m -> m.masterData().isPublished().is(true));
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, query);
    }

    private ItemWriter<Product> unpublishWriter() {
        return items -> items.stream()
                .peek(element -> LOGGER.debug("Attempting to delete product " + element.getId()))
                .map(product -> ProductUpdateCommand.of(product, Unpublish.of()))
                .forEach(sphereClient::executeBlocking);
    }

    private ItemStreamReader<Product> deleteReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of());
    }

    private ItemWriter<Product> deleteWriter() {
        return items -> items.forEach(item -> sphereClient.execute(ProductDeleteCommand.of(item)));
    }
}
