package com.commercetools.dataimport.channels;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class ChannelsDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job channelsDeleteJob(final Step channelsDeleteStep) {
        return jobBuilderFactory.get("channelsDeleteJob")
                .start(channelsDeleteStep)
                .build();
    }

    @Bean
    public Step channelsDeleteStep(final ItemStreamReader<Channel> deleteChannelReader, final ItemWriter<Channel> deleteChannelWriter) {
        return stepBuilderFactory.get("channelsDeleteStep")
                .<Channel, Channel>chunk(1)
                .reader(deleteChannelReader)
                .writer(deleteChannelWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<Channel> deleteChannelReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ChannelQuery.of());
    }

    @Bean
    public ItemWriter<Channel> deleteChannelWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(ChannelDeleteCommand.of(item)));
    }
}
