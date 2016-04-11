package com.commercetools.dataimport.products;

import com.commercetools.dataimport.commercetools.CommercetoolsPayloadFileConfig;
import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.SphereClientUtils;
import io.sphere.sdk.models.Versioned;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static io.sphere.sdk.client.SphereClientUtils.blockingWaitForEachCollector;
import static java.util.stream.Collectors.toList;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job productsDeleteJob(Step unpublishProducts, Step deleteProductsStep) {
        return jobBuilderFactory.get("productsDeleteJob")
                .start(unpublishProducts)
                .next(deleteProductsStep)
                .build();
    }

    @Bean
    protected Step unpublishProducts() {
        final ProductQuery query = ProductQuery.of().withPredicates(m -> m.masterData().isPublished().is(true));
        return stepBuilderFactory.get("unpublishProductsStep")
                .<Product, Product>chunk(50)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, query))
                .writer(productUnpublishWriter())
                .build();
    }

    @Bean
    protected Step deleteProductsStep() {
        return stepBuilderFactory.get("deleteProductsStep")
                .<Product, Product>chunk(50)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of()))
                .writer(productDeleteWriter())
                .build();
    }

    private ItemWriter<Versioned<Product>> productUnpublishWriter() {
        return items -> {
            final List<CompletionStage<Product>> completionStages = items.stream()
                    .map(item -> sphereClient.execute(ProductUpdateCommand.of(item, Unpublish.of())))
                    .collect(toList());
            completionStages.forEach(stage -> SphereClientUtils.blockingWait(stage, 60, TimeUnit.SECONDS));
        };
    }

    private ItemWriter<Versioned<Product>> productDeleteWriter() {
        return items -> items.stream()
                .map(item -> sphereClient.execute(ProductDeleteCommand.of(item)))
                .collect(blockingWaitForEachCollector(60, TimeUnit.SECONDS));
    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsPayloadFileConfig.class, ProductDeleteJobConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
