package com.commercetools.dataimport.orders;

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
public class OrderTypeImportJobConfiguration extends TypeImportJobConfiguration {

    @Bean
    public Job orderTypeImportJob(final Step orderTypeImportStep) {
        return jobBuilderFactory.get("orderTypeImportJob")
                .start(orderTypeImportStep)
                .build();
    }

    @Bean
    public Step orderTypeImportStep(final ItemReader<TypeDraft> typeReader, final ItemWriter<TypeDraft> typeWriter) {
        return stepBuilderFactory.get("orderTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(typeReader)
                .writer(typeWriter)
                .build();
    }
}
