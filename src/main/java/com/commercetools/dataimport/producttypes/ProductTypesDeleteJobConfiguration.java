package com.commercetools.dataimport.producttypes;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductTypesDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job productTypesDeleteJob(final Step productTypesDeleteStep) {
        return jobBuilderFactory.get("productTypesDeleteJob")
                .start(productTypesDeleteStep)
                .build();
    }

    @Bean
    public Step productTypesDeleteStep(final ItemStreamReader<ProductType> productTypesDeleteReader,
                                       final ItemWriter<ProductType> productTypesDeleteWriter) {
        return stepBuilderFactory.get("productTypesDeleteStep")
                .<ProductType, ProductType>chunk(1)
                .reader(productTypesDeleteReader)
                .writer(productTypesDeleteWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<ProductType> productTypesDeleteReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductTypeQuery.of());
    }

    @Bean
    public ItemWriter<ProductType> productTypesDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(ProductTypeDeleteCommand.of(item)));
    }
}
