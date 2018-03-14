package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataImportJobConfiguration {

    private static final String EXECUTE = "EXECUTE";

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job job(Flow projectCleanUpFlow, Flow catalogImportFlow, Step customerTypeImportStep,
                   Flow reserveInStoreImportFlow, Step ordersImportStep) {
        return jobBuilderFactory.get("dataImport")
                .start(projectCleanUpFlow)
                .next(catalogImportFlow)
                .next(customerTypeImportStep)
                .next(isFlagEnabled("reserveInStore")).on(EXECUTE).to(reserveInStoreImportFlow)
                .next(isFlagEnabled("orders")).on(EXECUTE).to(ordersImportStep)
                .end()
                .build();
    }

    @Bean
    public Flow reserveInStoreImportFlow(Step orderTypeImportStep, Step inventoryStoresImportStep) {
        return new FlowBuilder<Flow>("reserveInStoreImportFlow")
                .next(orderTypeImportStep)
                .next(inventoryStoresImportStep)
                .build();
    }

    @Bean
    public Flow catalogImportFlow(Step projectSettingsStep, Step productTypeImportStep, Step taxCategoryImportStep,
                                  Step channelTypeImportStep, Step channelsImportStep, Step customerGroupImportStep,
                                  Step categoriesImportStep, Step productsImportStep, Step inventoryImportStep) {
        return new FlowBuilder<Flow>("catalogImportFlow")
                .start(projectSettingsStep)
                .next(customerGroupImportStep)
                .next(categoriesImportStep)
                .next(taxCategoryImportStep)
                .next(channelTypeImportStep)
                .next(channelsImportStep)
                .next(productTypeImportStep)
                .next(productsImportStep)
                .next(inventoryImportStep)
                .build();
    }

    @Bean
    public Flow projectCleanUpFlow(Step ordersDeleteStep, Step cartsDeleteStep, Step shippingMethodsDeleteStep,
                                   Step productsDeleteStep, Step productTypeDeleteStep, Step inventoryDeleteStep,
                                   Step taxCategoryDeleteStep, Step customerGroupDeleteStep, Flow categoriesDeleteFlow,
                                   Step orderTypeDeleteStep, Step customerTypeDeleteStep, Step channelTypeDeleteStep,
                                   Step channelsDeleteStep) {
        return new FlowBuilder<Flow>("projectCleanUpFlow")
                .start(ordersDeleteStep)
                .next(cartsDeleteStep)
                .next(shippingMethodsDeleteStep)
                .next(inventoryDeleteStep)
                .next(productsDeleteStep)
                .next(productTypeDeleteStep)
                .next(taxCategoryDeleteStep)
                .next(categoriesDeleteFlow)
                .next(customerGroupDeleteStep)
                .next(channelsDeleteStep)
                .next(customerTypeDeleteStep)
                .next(orderTypeDeleteStep)
                .next(channelTypeDeleteStep)
                .build();
    }

    private JobExecutionDecider isFlagEnabled(final String flag) {
        return (j, s) -> j.getJobParameters().getString(flag) != null ? new FlowExecutionStatus(EXECUTE) : FlowExecutionStatus.STOPPED;

    }
}
