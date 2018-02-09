package com.commercetools.dataimport.channels;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ChannelTypeDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job channelTypeDeleteJob(final Step channelTypeDeleteStep) {
        return jobBuilderFactory.get("channelTypeDeleteJob")
                .start(channelTypeDeleteStep)
                .build();
    }

    @Bean
    public Step channelTypeDeleteStep(final ItemReader<Type> deleteChannelTypeReader, final ItemWriter<Type> deleteChannelTypeWriter) {
        return stepBuilderFactory.get("channelTypeDeleteStep")
                .<Type, Type>chunk(1)
                .reader(deleteChannelTypeReader)
                .writer(deleteChannelTypeWriter)
                .build();
    }

    @Bean
    public ItemReader<Type> deleteChannelTypeReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("channel"))));
    }

    @Bean
    public ItemWriter<Type> deleteChannelTypeWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(TypeDeleteCommand.of(item)));
    }
}
