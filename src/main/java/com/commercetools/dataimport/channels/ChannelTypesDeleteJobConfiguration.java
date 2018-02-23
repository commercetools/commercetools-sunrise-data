package com.commercetools.dataimport.channels;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
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

import static java.util.Collections.singletonList;

@Configuration
@EnableBatchProcessing
public class ChannelTypesDeleteJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    public Job channelTypesDeleteJob(final Step channelTypesDeleteStep) {
        return jobBuilderFactory.get("channelTypesDeleteJob")
                .start(channelTypesDeleteStep)
                .build();
    }

    @Bean
    @JobScope
    public Step channelTypesDeleteStep(final ItemWriter<Type> typeDeleteWriter) {
        return stepBuilderFactory.get("channelTypesDeleteStep")
                .<Type, Type>chunk(1)
                .reader(reader())
                .writer(typeDeleteWriter)
                .build();
    }

    private ItemReader<Type> reader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("channel"))));
    }
}
