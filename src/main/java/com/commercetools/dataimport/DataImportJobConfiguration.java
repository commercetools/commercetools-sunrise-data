package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataImportJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Value("${projectCleanUp}")
    private boolean projectCleanUpFlag;
    @Value("${projectSetUp}")
    private boolean projectSetUpFlag;
    @Value("${catalogImport}")
    private boolean catalogImportFlag;
    @Value("${categoryImport}")
    private boolean categoryImportFlag;
    @Value("${reserveInStore}")
    private boolean reserveInStoreFlag;
    @Value("${ordersImport}")
    private boolean ordersImportFlag;
    @Value("${channelsImport}")
    private boolean channelsImportFlag;

    @Bean
    public Job job(Flow projectCleanUpFlow, Flow projectSetUpFlow, Flow catalogImportFlow, Flow categoryImportFlow,
                   Flow reserveInStoreImportFlow, Flow ordersImportFlow, Flow channelsImportFlow) {
        return jobBuilderFactory.get("dataImport")
                .start(projectCleanUpFlow)
                .next(projectSetUpFlow)
                .next(channelsImportFlow)
                .next(categoryImportFlow)
                .next(catalogImportFlow)
                .next(reserveInStoreImportFlow)
                .next(ordersImportFlow)
                .end()
                .build();
    }

    @Bean
    public Flow projectCleanUpFlow(Step ordersDeleteStep, Step cartsDeleteStep, Step shippingMethodsDeleteStep,
                                   Step productsDeleteStep, Step productTypeDeleteStep, Step inventoryDeleteStep,
                                   Step taxCategoryDeleteStep, Step customerGroupDeleteStep, Flow categoriesDeleteFlow,
                                   Step orderTypeDeleteStep, Step customerTypeDeleteStep, Step channelTypeDeleteStep,
                                   Step channelsDeleteStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(projectCleanUpFlag);
        return new FlowBuilder<Flow>("projectCleanUpFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(ordersDeleteStep)
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
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                    .end()
                .build();
    }

    @Bean
    public Flow projectSetUpFlow(Step projectSettingsStep, Step customerTypeImportStep,
                                 Step taxCategoryImportStep, Step customerGroupImportStep,
                                 Step channelTypeImportStep, Step productTypeImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(projectSetUpFlag);
        return new FlowBuilder<Flow>("projectSetUpFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(projectSettingsStep)
                    .next(customerTypeImportStep)
                    .next(customerGroupImportStep)
                    .next(taxCategoryImportStep)
                    .next(channelTypeImportStep)
                    .next(productTypeImportStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                    .end()
                .build();
    }

    @Bean
    public Flow catalogImportFlow(Step taxCategoryImportStep, Step customerGroupImportStep,
                                  Step channelTypeImportStep, Step channelsImportStep,
                                  Step productTypeImportStep, Step productsImportStep, Step inventoryImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(catalogImportFlag);
        return new FlowBuilder<Flow>("catalogImportFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(customerGroupImportStep)
                    .next(taxCategoryImportStep)
                    .next(channelTypeImportStep)
                    .next(channelsImportStep)
                    .next(productTypeImportStep)
                    .next(productsImportStep)
                    .next(inventoryImportStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                    .end()
                .build();
    }


    @Bean
    public Flow categoryImportFlow(Step categoriesImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(categoryImportFlag);
        return new FlowBuilder<Flow>("categoryImportFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                .to(categoriesImportStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                .end()
                .build();
    }



    @Bean
    public Flow reserveInStoreImportFlow(Step orderTypeImportStep, Step inventoryStoresImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(reserveInStoreFlag);
        return new FlowBuilder<Flow>("reserveInStoreImportFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(orderTypeImportStep)
                    .next(inventoryStoresImportStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                    .end()
                .build();
    }

    @Bean
    public Flow channelsImportFlow(Step channelsImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(channelsImportFlag);
        return new FlowBuilder<Flow>("channelsImportFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                .to(channelsImportStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                .end()
                .build();
    }

    @Bean
    public Flow ordersImportFlow(Step ordersDeleteStep, Step ordersImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(ordersImportFlag);
        return new FlowBuilder<Flow>("ordersImportFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(ordersDeleteStep)
                    .next(ordersImportStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                    .end()
                .build();
    }
}
