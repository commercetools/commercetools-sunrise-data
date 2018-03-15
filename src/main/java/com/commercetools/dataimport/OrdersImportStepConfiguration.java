package com.commercetools.dataimport;

import com.commercetools.dataimport.orders.OrderCsvEntry;
import com.commercetools.dataimport.orders.OrderImportItemProcessor;
import com.commercetools.dataimport.orders.OrderImportItemReader;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.commands.OrderImportCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
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

import java.util.List;
import java.util.concurrent.Future;

@Configuration
@Slf4j
public class OrdersImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${chunkSize}")
    private int chunkSize;

    @Value("${maxThreads}")
    private int maxThreads;

    @Value("${resource.orders}")
    private Resource ordersResource;

    @Value("${resource.orderType}")
    private Resource orderTypeResource;

    @Bean
    public Step ordersImportStep() throws Exception {
        return stepBuilderFactory.get("ordersImportStep")
                .<List<OrderCsvEntry>, Future<OrderImportCommand>>chunk(chunkSize)
                .reader(new OrderImportItemReader(ordersResource))
                .processor(ctpBatch.asyncProcessor(new OrderImportItemProcessor()))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step orderTypeImportStep() throws Exception {
        return stepBuilderFactory.get("orderTypeImportStep")
                .<TypeDraft, Future<TypeCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.jsonReader(orderTypeResource, TypeDraft.class))
                .processor(ctpBatch.asyncProcessor(TypeCreateCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step orderTypeDeleteStep() throws Exception {
        return stepBuilderFactory.get("orderTypeDeleteStep")
                .<Type, Future<TypeDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.typeQueryReader("order"))
                .processor(ctpBatch.asyncProcessor(TypeDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step ordersDeleteStep() throws Exception {
        return stepBuilderFactory.get("ordersDeleteStep")
                .<Order, Future<OrderDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(OrderQuery.of()))
                .processor(ctpBatch.asyncProcessor(OrderDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step cartsDeleteStep() throws Exception {
        return stepBuilderFactory.get("cartsDeleteStep")
                .<Cart, Future<CartDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(CartQuery.of()))
                .processor(ctpBatch.asyncProcessor(CartDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }
}
