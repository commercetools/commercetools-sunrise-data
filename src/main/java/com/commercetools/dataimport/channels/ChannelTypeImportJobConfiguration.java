package com.commercetools.dataimport.channels;

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
public class ChannelTypeImportJobConfiguration extends TypeImportJobConfiguration {

    @Bean
    public Job channelTypeImportJob(final Step channelTypeImportStep) {
        return jobBuilderFactory.get("channelTypeImportJob")
                .start(channelTypeImportStep)
                .build();
    }

    @Bean
    public Step channelTypeImportStep(final ItemReader<TypeDraft> importTypeReader, final ItemWriter<TypeDraft> importTypeWriter) {
        return stepBuilderFactory.get("channelTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(importTypeReader)
                .writer(importTypeWriter)
                .build();
    }
}
