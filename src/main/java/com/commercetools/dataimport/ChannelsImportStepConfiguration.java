package com.commercetools.dataimport;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.queries.TypeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static java.util.Collections.singletonList;

@Configuration
public class ChannelsImportStepConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelsImportStepConfiguration.class);

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    @JobScope
    public Step channelTypeDeleteStep(ItemWriter<Type> typeDeleteWriter) {
        return stepBuilderFactory.get("channelTypeDeleteStep")
                .<Type, Type>chunk(1)
                .reader(channelTypeDeleteStepReader())
                .writer(typeDeleteWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step channelTypeImportStep(ItemReader<TypeDraft> channelTypeImportStepReader,
                                      ItemWriter<TypeDraft> typeImportWriter) {
        return stepBuilderFactory.get("channelTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(channelTypeImportStepReader)
                .writer(typeImportWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step channelsImportStep(ItemReader<ChannelDraft> channelsImportStepReader) {
        return stepBuilderFactory.get("channelsImportStep")
                .<ChannelDraft, ChannelDraft>chunk(1)
                .reader(channelsImportStepReader)
                .writer(channelsImportStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step channelsDeleteStep() {
        return stepBuilderFactory.get("channelsDeleteStep")
                .<Channel, Channel>chunk(1)
                .reader(channelsDeleteStepReader())
                .writer(channelsDeleteStepWriter())
                .build();
    }

    private ItemReader<Type> channelTypeDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("channel"))));
    }

    @Bean
    @StepScope
    public ListItemReader<ChannelDraft> channelsImportStepReader(@Value("${resource.channels}") Resource resource) throws IOException {
        return JsonUtils.createJsonListReader(resource, ChannelDraft.class);
    }

    @Bean
    @StepScope
    public ListItemReader<TypeDraft> channelTypeImportStepReader(@Value("${resource.channelType}") Resource resource) throws IOException {
        return JsonUtils.createJsonListReader(resource, TypeDraft.class);
    }

    private ItemWriter<ChannelDraft> channelsImportStepWriter() {
        return items -> items.forEach(channelDraft -> {
            final Channel channel = sphereClient.executeBlocking(ChannelCreateCommand.of(channelDraft));
            LOGGER.debug("Created channel \"{}\"", channel.getKey());
        });
    }

    private ItemStreamReader<Channel> channelsDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ChannelQuery.of());
    }

    private ItemWriter<Channel> channelsDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final Channel channel = sphereClient.executeBlocking(ChannelDeleteCommand.of(item));
            LOGGER.debug("Removed channel \"{}\"", channel.getKey());
        });
    }
}
