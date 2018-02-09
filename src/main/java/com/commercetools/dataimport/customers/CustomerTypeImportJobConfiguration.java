package com.commercetools.dataimport.customers;

import com.commercetools.dataimport.TypeImportJobConfiguration;
import io.sphere.sdk.types.TypeDraft;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class CustomerTypeImportJobConfiguration extends TypeImportJobConfiguration {

    @Bean
    public Job customerTypeImportJob(final Step customerTypeImportStep) {
        return jobBuilderFactory.get("customerTypeImportJob")
                .start(customerTypeImportStep)
                .build();
    }

    @Bean
    public Step customerTypeImportStep(final ItemReader<TypeDraft> typeReader, final ItemWriter<TypeDraft> typeWriter) {
        return stepBuilderFactory.get("customerTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(typeReader)
                .writer(typeWriter)
                .build();
    }
}
