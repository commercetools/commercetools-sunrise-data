package com.commercetools.dataimport.customers;

import io.sphere.sdk.types.TypeDraft;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class CustomerTypesImportJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job customerTypesImportJob(final Step customerTypesImportStep) {
        return jobBuilderFactory.get("customerTypesImportJob")
                .start(customerTypesImportStep)
                .build();
    }

    @Bean
    public Step customerTypesImportStep(final ItemReader<TypeDraft> typeImportReader,
                                        final ItemWriter<TypeDraft> typeImportWriter) {
        return stepBuilderFactory.get("customerTypesImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(typeImportReader)
                .writer(typeImportWriter)
                .build();
    }
}
