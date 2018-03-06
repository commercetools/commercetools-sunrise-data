package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrdersImportJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job ordersImport(Step ordersDeleteStep, Step ordersImportStep) {
        return jobBuilderFactory.get("ordersImport")
                .start(ordersDeleteStep)
                .next(ordersImportStep)
                .build();
    }
}
