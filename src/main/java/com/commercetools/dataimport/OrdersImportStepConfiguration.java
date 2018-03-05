package com.commercetools.dataimport;

import com.commercetools.dataimport.orders.OrderCsvEntry;
import com.commercetools.dataimport.orders.OrderImportItemReader;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.orders.*;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.commands.OrderImportCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.MoneyImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Configuration
@Slf4j
public class OrdersImportStepConfiguration {

    private static final String[] ORDER_CSV_HEADER_NAMES = new String[]{"customerEmail", "orderNumber", "lineitems.variant.sku", "lineitems.price", "lineitems.quantity", "totalPrice"};

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
                .reader(ordersImportStepReader())
                .processor(ordersImportStepProcessor())
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

    private ItemReader<TypeDraft> orderTypeImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(orderTypeResource, TypeDraft.class);
    }

    private ItemReader<Order> ordersDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, OrderQuery.of());
    }

    private ItemWriter<Order> ordersDeleteStepWriter() {
        return items -> items.forEach(item -> sphereClient.executeBlocking(OrderDeleteCommand.of(item)));
    }

    private ItemReader<Type> orderTypeDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("order"))));
    }

    private ItemProcessor<List<OrderCsvEntry>, OrderImportDraft> ordersImportStepProcessor() {
        return items -> {
            if (!items.isEmpty()) {
                final OrderCsvEntry firstCsvLine = items.get(0);
                final MonetaryAmount totalPrice = centAmountToMoney(firstCsvLine.getTotalPrice());
                final OrderState state = OrderState.COMPLETE;
                final List<LineItemImportDraft> lineItemImportDrafts = items.stream()
                        .map(this::lineItemToDraft)
                        .collect(toList());
                return OrderImportDraftBuilder.ofLineItems(totalPrice, state, lineItemImportDrafts)
                        .customerEmail(firstCsvLine.getCustomerEmail())
                        .orderNumber(firstCsvLine.getOrderNumber())
                        .build();
            }
            return null;
        };
    }

    private LineItemImportDraft lineItemToDraft(final OrderCsvEntry item) {
        final ProductVariantImportDraft draft = ProductVariantImportDraftBuilder.ofSku(item.getLineItems().getVariant().getSku()).build();
        final long quantity = item.getLineItems().getQuantity();
        final Price price = Price.of(centAmountToMoney(item.getLineItems().getPrice()));
        final LocalizedString name = LocalizedString.ofEnglish("Product Name");
        return LineItemImportDraftBuilder.of(draft, quantity, price, name).build();
    }

    private static MonetaryAmount centAmountToMoney(final double centAmount) {
        return MoneyImpl.ofCents((long) (centAmount * 100), "EUR");
    }

    private ItemWriter<OrderImportDraft> ordersImportStepWriter() {
        return items -> items.forEach(item -> {
            final Order order = sphereClient.executeBlocking(OrderImportCommand.of(item));
            log.debug("Created order \"{}\"", order.getOrderNumber());
        });
    }

    private ItemReader<List<OrderCsvEntry>> ordersImportStepReader() {
        final OrderImportItemReader reader = new OrderImportItemReader();
        final SingleItemPeekableItemReader<OrderCsvEntry> itemReader = new SingleItemPeekableItemReader<>();
        itemReader.setDelegate(flatFileItemReader(ordersResource));
        reader.setItemReader(itemReader);
        return reader;
    }

    private static ItemReader<OrderCsvEntry> flatFileItemReader(final Resource resource) {
        final FlatFileItemReader<OrderCsvEntry> reader = new FlatFileItemReader<>();
        reader.setLineMapper(lineMapper());
        reader.setLinesToSkip(1);
        reader.setResource(resource);
        return reader;
    }

    private static DefaultLineMapper<OrderCsvEntry> lineMapper() {
        return new DefaultLineMapper<OrderCsvEntry>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(ORDER_CSV_HEADER_NAMES);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<OrderCsvEntry>() {{
                setTargetType(OrderCsvEntry.class);
            }});
        }};
    }
}
