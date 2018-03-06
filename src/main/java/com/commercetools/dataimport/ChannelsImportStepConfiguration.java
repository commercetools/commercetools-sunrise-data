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
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static java.util.Collections.singletonList;

@Configuration
@Slf4j
public class ChannelsImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.channels}")
    private Resource channelsResource;

    @Value("${resource.channelType}")
    private Resource channelTypeResource;

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
    public Step channelTypeImportStep(ItemWriter<TypeDraft> typeImportWriter) throws IOException {
        return stepBuilderFactory.get("channelTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(channelTypeImportStepReader())
                .writer(typeImportWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step channelsImportStep() throws IOException {
        return stepBuilderFactory.get("channelsImportStep")
                .<ChannelDraft, ChannelDraft>chunk(1)
                .reader(channelsImportStepReader())
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

    private ItemReader<ChannelDraft> channelsImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(channelsResource, ChannelDraft.class);
    }

    private ItemReader<TypeDraft> channelTypeImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(channelTypeResource, TypeDraft.class);
    }

    private ItemWriter<ChannelDraft> channelsImportStepWriter() {
        return items -> items.forEach(channelDraft -> {
            final Channel channel = sphereClient.executeBlocking(ChannelCreateCommand.of(channelDraft));
            log.debug("Created channel \"{}\"", channel.getKey());
        });
    }

    private ItemReader<Channel> channelsDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ChannelQuery.of());
    }

    private ItemWriter<Channel> channelsDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final Channel channel = sphereClient.executeBlocking(ChannelDeleteCommand.of(item));
            log.debug("Removed channel \"{}\"", channel.getKey());
        });
    }
}
