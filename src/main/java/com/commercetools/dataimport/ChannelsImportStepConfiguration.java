package com.commercetools.dataimport;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.concurrent.Future;

@Configuration
@Slf4j
public class ChannelsImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${resource.channels}")
    private Resource channelsResource;

    @Value("${resource.channelType}")
    private Resource channelTypeResource;

    @Bean
    public Step channelTypeDeleteStep() throws Exception {
        return stepBuilderFactory.get("channelTypeDeleteStep")
                .<Type, Future<TypeDeleteCommand>>chunk(1)
                .reader(ctpBatch.typeQueryReader("channel"))
                .processor(ctpBatch.asyncProcessor(TypeDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .build();
    }

    @Bean
    public Step channelsDeleteStep() throws Exception {
        return stepBuilderFactory.get("channelsDeleteStep")
                .<Channel, Future<ChannelDeleteCommand>>chunk(1)
                .reader(ctpBatch.queryReader(ChannelQuery.of()))
                .processor(ctpBatch.asyncProcessor(ChannelDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .build();
    }

    @Bean
    public Step channelTypeImportStep() throws Exception {
        return stepBuilderFactory.get("channelTypeImportStep")
                .<TypeDraft, Future<TypeCreateCommand>>chunk(1)
                .reader(ctpBatch.jsonReader(channelTypeResource, TypeDraft.class))
                .processor(ctpBatch.asyncProcessor(TypeCreateCommand::of))
                .writer(ctpBatch.asyncWriter())
                .build();
    }

    @Bean
    public Step channelsImportStep() throws Exception {
        return stepBuilderFactory.get("channelsImportStep")
                .<ChannelDraft, Future<ChannelCreateCommand>>chunk(1)
                .reader(ctpBatch.jsonReader(channelsResource, ChannelDraft.class))
                .processor(ctpBatch.asyncProcessor(ChannelCreateCommand::of))
                .writer(ctpBatch.asyncWriter())
                .build();
    }
}
