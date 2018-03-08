package com.commercetools.dataimport;

import com.commercetools.dataimport.orders.OrderCsvEntry;
import com.commercetools.dataimport.orders.OrderImportItemProcessor;
import com.commercetools.dataimport.orders.OrderImportItemReader;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.OrderImportDraft;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.commands.OrderImportCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
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
import java.util.List;

import static java.util.Collections.singletonList;

@Configuration
@Slf4j
public class OrdersImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.orders}")
    private Resource ordersResource;

    @Value("${resource.orderType}")
    private Resource orderTypeResource;

    @Bean
    @JobScope
    public Step ordersImportStep() {
        return stepBuilderFactory.get("ordersImportStep")
                .<List<OrderCsvEntry>, OrderImportDraft>chunk(1)
                .reader(new OrderImportItemReader(ordersResource))
                .processor(new OrderImportItemProcessor())
                .writer(ordersImportStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step orderTypeImportStep(ItemWriter<TypeDraft> typeImportWriter) throws IOException {
        return stepBuilderFactory.get("orderTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(orderTypeImportStepReader())
                .writer(typeImportWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step orderTypeDeleteStep(ItemWriter<Type> typeDeleteWriter) {
        return stepBuilderFactory.get("orderTypeDeleteStep")
                .<Type, Type>chunk(1)
                .reader(orderTypeDeleteStepReader())
                .writer(typeDeleteWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step ordersDeleteStep() {
        return stepBuilderFactory.get("ordersDeleteStep")
                .<Order, Order>chunk(1)
                .reader(ordersDeleteStepReader())
                .writer(ordersDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step cartsDeleteStep() {
        return stepBuilderFactory.get("cartsDeleteStep")
                .<Cart, Cart>chunk(1)
                .reader(cartsDeleteStepReader())
                .writer(cartsDeleteStepWriter())
                .build();
    }

    private ItemReader<TypeDraft> orderTypeImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(orderTypeResource, TypeDraft.class);
    }

    private ItemReader<Order> ordersDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, OrderQuery.of());
    }

    private ItemWriter<Order> ordersDeleteStepWriter() {
        return items -> items.forEach(item -> sphereClient.executeBlocking(OrderDeleteCommand.of(item)));
    }

    private ItemReader<Cart> cartsDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CartQuery.of());
    }

    private ItemWriter<Cart> cartsDeleteStepWriter() {
        return items -> items.forEach(item -> sphereClient.executeBlocking(CartDeleteCommand.of(item)));
    }

    private ItemReader<Type> orderTypeDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("order"))));
    }

    private ItemWriter<OrderImportDraft> ordersImportStepWriter() {
        return items -> items.forEach(item -> {
            final Order order = sphereClient.executeBlocking(OrderImportCommand.of(item));
            log.debug("Created order \"{}\"", order.getOrderNumber());
        });
    }
}
