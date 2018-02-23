package com.commercetools.dataimport.orders;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class OrdersDeleteJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    public Job ordersDeleteJob() {
        return jobBuilderFactory.get("ordersDeleteJob")
                .start(ordersDeleteStep())
                .build();
    }

    @Bean
    @JobScope
    public Step ordersDeleteStep() {
        return stepBuilderFactory.get("ordersDeleteStep")
                .<Order, Order>chunk(1)
                .reader(reader())
                .writer(writer())
                .build();
    }

    private ItemStreamReader<Order> reader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, OrderQuery.of());
    }

    private ItemWriter<Order> writer() {
        return items -> items.forEach(item -> sphereClient.executeBlocking(OrderDeleteCommand.of(item)));
    }
}
