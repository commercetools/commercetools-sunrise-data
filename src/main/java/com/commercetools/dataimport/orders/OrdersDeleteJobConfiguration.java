package com.commercetools.dataimport.orders;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrdersDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job ordersDeleteJob(final Step ordersDeleteStep) {
        return jobBuilderFactory.get("ordersDeleteJob")
                .start(ordersDeleteStep)
                .build();
    }

    @Bean
    public Step ordersDeleteStep(final ItemStreamReader<Order> ordersDeleteReader,
                                 final ItemWriter<Order> ordersDeleteWriter) {
        return stepBuilderFactory.get("ordersDeleteStep")
                .<Order, Order>chunk(1)
                .reader(ordersDeleteReader)
                .writer(ordersDeleteWriter)
                .build();
    }

    @Bean
    public ItemStreamReader<Order> ordersDeleteReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, OrderQuery.of());
    }

    @Bean
    public ItemWriter<Order> ordersDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(OrderDeleteCommand.of(item)));
    }
}
