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
    @Value("${reserveInStore}")
    private boolean reserveInStoreFlag;
    @Value("${ordersImport}")
    private boolean ordersImportFlag;

    @Bean
    public Job job(Flow projectCleanUpFlow, Flow projectSetUpFlow, Flow catalogImportFlow,
                   Step customerTypeImportStep, Flow reserveInStoreImportFlow, Flow ordersImportFlow) {
        return jobBuilderFactory.get("dataImport")
                .start(projectCleanUpFlow)
                .next(projectSetUpFlow)
                .next(catalogImportFlow)
                .next(customerTypeImportStep)
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
    public Flow projectSetUpFlow(Step projectSettingsStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(projectSetUpFlag);
        return new FlowBuilder<Flow>("projectSetUpFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(projectSettingsStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                    .end()
                .build();
    }

    @Bean
    public Flow catalogImportFlow(Step productTypeImportStep, Step taxCategoryImportStep,
                                  Step channelTypeImportStep, Step channelsImportStep, Step customerGroupImportStep,
                                  Step categoriesImportStep, Step productsImportStep, Step inventoryImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(catalogImportFlag);
        return new FlowBuilder<Flow>("catalogImportFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(customerGroupImportStep)
                    .next(categoriesImportStep)
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
    public Flow ordersImportFlow(Step ordersImportStep) {
        final FlagFlowDecider flagFlowDecider = new FlagFlowDecider(ordersImportFlag);
        return new FlowBuilder<Flow>("ordersImportFlow")
                .start(flagFlowDecider).on(FlagFlowDecider.RUN)
                    .to(ordersImportStep)
                .from(flagFlowDecider).on(FlagFlowDecider.SKIP)
                    .end()
                .build();
    }
}
