package com.commercetools.dataimport.orders;

import com.commercetools.dataimport.TypesImportJobConfiguration;
import io.sphere.sdk.types.TypeDraft;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderTypesImportJobConfiguration extends TypesImportJobConfiguration {

    @Bean
    public Job orderTypesImportJob(final Step orderTypesImportStep) {
        return jobBuilderFactory.get("orderTypesImportJob")
                .start(orderTypesImportStep)
                .build();
    }

    @Bean
    public Step orderTypesImportStep(final ItemReader<TypeDraft> typeImportReader,
                                    final ItemWriter<TypeDraft> typeImportWriter) {
        return stepBuilderFactory.get("orderTypesImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(typeImportReader)
                .writer(typeImportWriter)
                .build();
    }
}
