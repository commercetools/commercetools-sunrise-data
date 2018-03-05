package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataImportJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job dataImport(Step productsUnpublishStep, Step productsDeleteStep,
                          Step productTypeDeleteStep, Step taxCategoryDeleteStep, Step customerGroupDeleteStep,
                          Step orderTypeDeleteStep, Step customerTypeDeleteStep,
                          Step channelTypeImportStep, Step channelsImportStep,
                          Step channelTypeDeleteStep, Step channelsDeleteStep,
                          Step customerTypeImportStep, Step orderTypeImportStep,
                          Step productTypeImportStep, Step taxCategoryImportStep, Step customerGroupImportStep) {
        return jobBuilderFactory.get("dataImport")
                // products (delete)
                .start(productsUnpublishStep)
                .next(productsDeleteStep)
                .next(productTypeDeleteStep)
                .next(taxCategoryDeleteStep)
                .next(customerGroupDeleteStep)
                // order type
                .next(orderTypeDeleteStep)
                .next(orderTypeImportStep)
                // customer type
                .next(customerTypeDeleteStep)
                .next(customerTypeImportStep)
                // channels
                .next(channelsDeleteStep)
                .next(channelTypeDeleteStep)
                .next(channelTypeImportStep)
                .next(channelsImportStep)
                // products (import)
                .next(customerGroupImportStep)
                .next(taxCategoryImportStep)
                .next(productTypeImportStep)
                .build();
    }
}
