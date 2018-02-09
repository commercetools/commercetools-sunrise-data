package com.commercetools.dataimport.channels;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.JsonUtils;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
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
public class ChannelsImportJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job channelsImportJob(final Step channelsImportStep) {
        return jobBuilderFactory.get("channelsImportJob")
                .start(channelsImportStep)
                .build();
    }

    @Bean
    public Step channelsImportStep(final ItemReader<ChannelDraft> importChannelReader, final ItemWriter<ChannelDraft> importChannelWriter) {
        return stepBuilderFactory.get("channelsImportStep")
                .<ChannelDraft, ChannelDraft>chunk(1)
                .reader(importChannelReader)
                .writer(importChannelWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ChannelDraft> importChannelReader(@Value("#{jobParameters['resource']}") final Resource resource) throws IOException {
        return JsonUtils.createJsonListReader(resource, ChannelDraft.class);
    }

    @Bean
    public ItemWriter<ChannelDraft> importChannelWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(channelDraft -> sphereClient.executeBlocking(ChannelCreateCommand.of(channelDraft)));
    }
}
