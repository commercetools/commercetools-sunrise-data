package com.commercetools.dataimport.producttypes;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ProductTypesDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job productTypesDeleteJob(final Step productTypesDeleteStep) {
        return jobBuilderFactory.get("productTypesDeleteJob")
                .start(productTypesDeleteStep)
                .build();
    }

    @Bean
    public Step productTypesDeleteStep(final ItemStreamReader<ProductType> productTypeReader, final ItemWriter<ProductType> productTypeWriter) {
        return stepBuilderFactory.get("productTypesDeleteStep")
                .<ProductType, ProductType>chunk(1)
                .reader(productTypeReader)
                .writer(productTypeWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<ProductType> productTypeReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductTypeQuery.of());
    }

    @Bean
    public ItemWriter<ProductType> productTypeWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(ProductTypeDeleteCommand.of(item)));
    }
}
