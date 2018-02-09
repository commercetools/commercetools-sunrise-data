package com.commercetools.dataimport.channels;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;

@Configuration
public class ChannelTypesDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job channelTypesDeleteJob(final Step channelTypesDeleteStep) {
        return jobBuilderFactory.get("channelTypesDeleteJob")
                .start(channelTypesDeleteStep)
                .build();
    }

    @Bean
    public Step channelTypesDeleteStep(final ItemReader<Type> channelTypesDeleteReader,
                                       final ItemWriter<Type> channelTypesDeleteWriter) {
        return stepBuilderFactory.get("channelTypesDeleteStep")
                .<Type, Type>chunk(1)
                .reader(channelTypesDeleteReader)
                .writer(channelTypesDeleteWriter)
                .build();
    }

    @Bean
    public ItemReader<Type> channelTypesDeleteReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("channel"))));
    }

    @Bean
    public ItemWriter<Type> channelTypesDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(TypeDeleteCommand.of(item)));
    }
}
