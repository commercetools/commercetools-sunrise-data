package com.commercetools.dataimport.all;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
@Lazy
public class ImportAllJobConfiguration {
    @Autowired
    protected JobBuilderFactory jobBuilderFactory;

    @Autowired
    protected StepBuilderFactory stepBuilderFactory;

    @Bean
    Job allImportsJob(Job importProducts, Job importCategories, Job productTypeCreateJob) {
        return jobBuilderFactory.get("allImportsJob")
                .start(stepBuilderFactory.get("importCategoriesJob").job(importCategories).build())
                .next(stepBuilderFactory.get("productTypeCreateJob").job(productTypeCreateJob).build())
                .next(stepBuilderFactory.get("importProductsJob").job(importProducts).build())
                .build();
    }
}
