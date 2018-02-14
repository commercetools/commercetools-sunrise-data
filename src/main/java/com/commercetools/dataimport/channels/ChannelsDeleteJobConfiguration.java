package com.commercetools.dataimport.channels;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class ChannelsDeleteJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    public Job channelsDeleteJob() {
        return jobBuilderFactory.get("channelsDeleteJob")
                .start(channelsDeleteStep())
                .build();
    }

    @Bean
    public Step channelsDeleteStep() {
        return stepBuilderFactory.get("channelsDeleteStep")
                .<Channel, Channel>chunk(1)
                .reader(reader())
                .writer(writer())
                .build();
    }

    private ItemStreamReader<Channel> reader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ChannelQuery.of());
    }

    private ItemWriter<Channel> writer() {
        return items -> items.forEach(item -> sphereClient.executeBlocking(ChannelDeleteCommand.of(item)));
    }
}
