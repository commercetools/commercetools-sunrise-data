package com.commercetools.dataimport.channels;

import io.sphere.sdk.types.TypeDraft;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class ChannelTypesImportJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job channelTypesImportJob(final Step channelTypesImportStep) {
        return jobBuilderFactory.get("channelTypesImportJob")
                .start(channelTypesImportStep)
                .build();
    }

    @Bean
    @JobScope
    public Step channelTypesImportStep(final ItemReader<TypeDraft> typeImportReader,
                                       final ItemWriter<TypeDraft> typeImportWriter) {
        return stepBuilderFactory.get("channelTypesImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(typeImportReader)
                .writer(typeImportWriter)
                .build();
    }
}
