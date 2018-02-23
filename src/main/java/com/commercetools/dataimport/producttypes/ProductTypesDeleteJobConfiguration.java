package com.commercetools.dataimport.producttypes;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
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
public class ProductTypesDeleteJobConfiguration {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProductTypesDeleteJobConfiguration.class);

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    public Job productTypesDeleteJob() {
        return jobBuilderFactory.get("productTypesDeleteJob")
                .start(productTypesDeleteStep())
                .build();
    }

    @Bean
    @JobScope
    public Step productTypesDeleteStep() {
        return stepBuilderFactory.get("productTypesDeleteStep")
                .<ProductType, ProductType>chunk(1)
                .reader(reader())
                .writer(writer())
                .build();
    }

    private ItemStreamReader<ProductType> reader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductTypeQuery.of());
    }

    private ItemWriter<ProductType> writer() {
        return items -> items.forEach(item -> {
            final ProductType productType = sphereClient.executeBlocking(ProductTypeDeleteCommand.of(item));
            LOGGER.debug("Removed product type \"{}\"", productType.getName());
        });
    }
}
