package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChannelsImportJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job channelsImport(Step productsUnpublishStep, Step productsDeleteStep, Step productTypeDeleteStep,
                              Step orderTypeDeleteStep, Step customerTypeDeleteStep,
                              Step channelTypeImportStep, Step channelsImportStep,
                              Step channelTypeDeleteStep, Step channelsDeleteStep,
                              Step customerTypeImportStep, Step orderTypeImportStep) {
        return jobBuilderFactory.get("channelsImport")
                // products (delete)
                .start(productsUnpublishStep)
                .next(productsDeleteStep)
                .next(productTypeDeleteStep)
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
                .build();
    }
}
