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
    public Job dataImport(Step ordersDeleteStep, Step cartsDeleteStep, Step shippingMethodsDeleteStep,
                          Step productsUnpublishStep, Step productsDeleteStep,
                          Step productTypeDeleteStep, Step taxCategoryDeleteStep, Step customerGroupDeleteStep,
                          Step rootCategoriesDeleteStep, Step remainingCategoriesDeleteStep,
                          Step orderTypeDeleteStep, Step customerTypeDeleteStep,
                          Step projectSettingsStep,
                          Step channelTypeImportStep, Step channelsImportStep,
                          Step channelTypeDeleteStep, Step channelsDeleteStep,
                          Step customerTypeImportStep, Step orderTypeImportStep,
                          Step productTypeImportStep, Step taxCategoryImportStep,
                          Step customerGroupImportStep, Step categoriesImportStep) {
        return jobBuilderFactory.get("dataImport")
                // DELETE
                // orders
                .start(ordersDeleteStep)
                .next(cartsDeleteStep)
                .next(shippingMethodsDeleteStep)
                // products
                .next(productsUnpublishStep)
                .next(productsDeleteStep)
                .next(productTypeDeleteStep)
                .next(taxCategoryDeleteStep)
                // categories
                .next(rootCategoriesDeleteStep)
                .next(remainingCategoriesDeleteStep)
                // customer group
                .next(customerGroupDeleteStep)
                // channels
                .next(channelsDeleteStep)
                // types
                .next(customerTypeDeleteStep)
                .next(orderTypeDeleteStep)
                .next(channelTypeDeleteStep)

                // IMPORT
                // project
                .next(projectSettingsStep)
                // types
                .next(channelTypeImportStep)
                .next(orderTypeImportStep)
                .next(customerTypeImportStep)
                // channels
                .next(channelsImportStep)
                // customer group
                .next(customerGroupImportStep)
                // categories
                .next(categoriesImportStep)
                // products
                .next(taxCategoryImportStep)
                .next(productTypeImportStep)
                .build();
    }
}
