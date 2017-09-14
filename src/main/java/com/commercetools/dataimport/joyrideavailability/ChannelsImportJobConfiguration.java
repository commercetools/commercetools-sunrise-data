package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.dataimport.common.JsonUtils;
import io.sphere.sdk.channels.ChannelDraftDsl;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Lazy
public class ChannelsImportJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ChannelsImportJobConfiguration.class);

    @Bean
    public Job importJoyrideChannelsJob(final Step customTypesImportStep,
                                        final Step channelsImportStep) {
        return jobBuilderFactory.get("importJoyrideChannelsJob")
                .start(customTypesImportStep)
                .next(channelsImportStep)
                .build();
    }

    @Bean
    public Step customTypesImportStep(final BlockingSphereClient sphereClient,
                                     final ItemReader<TypeDraft> customTypeReader) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("customTypeImportStep");
        return stepBuilder
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(customTypeReader)
                .writer(customTypeWriter(sphereClient))
                .build();
    }

    @Bean
    public Step channelsImportStep(final BlockingSphereClient sphereClient,
                                   final ItemReader<ChannelDraftDsl> channelsDraftReader) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("channelImportStep");
        return stepBuilder
                .<ChannelDraftDsl, ChannelDraftDsl>chunk(1)
                .reader(channelsDraftReader)
                .writer(channelsDraftWriter(sphereClient))
                .build();
    }

    @Bean
    @StepScope
    private ItemReader<TypeDraft> customTypeReader(@Value("#{jobParameters['typesResource']}") final Resource typesJsonResource) throws IOException {
        return JsonUtils.createJsonListReader(typesJsonResource, TypeDraft.class);
    }

    @Bean
    protected ItemWriter<TypeDraft> customTypeWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(customType -> sphereClient.executeBlocking(TypeCreateCommand.of(customType)));
    }

    @Bean
    @StepScope
    private ItemReader<ChannelDraftDsl> channelsDraftReader(@Value("#{jobParameters['channelsResource']}") final Resource channelsJsonResource) throws IOException {
        return JsonUtils.createJsonListReader(channelsJsonResource, ChannelDraftDsl.class);
    }

    @Bean
    protected ItemWriter<ChannelDraftDsl> channelsDraftWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(channelDraft -> sphereClient.executeBlocking(ChannelCreateCommand.of(channelDraft)));
    }
}
