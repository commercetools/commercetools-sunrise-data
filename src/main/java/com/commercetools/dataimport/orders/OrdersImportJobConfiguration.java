package com.commercetools.dataimport.orders;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.orders.cvsline.OrderCsvLineValue;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.orders.*;
import io.sphere.sdk.orders.commands.OrderImportCommand;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.utils.MoneyImpl;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.money.MonetaryAmount;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Configuration
public class OrdersImportJobConfiguration extends CommercetoolsJobConfiguration {

    private static final String[] ORDER_CSV_HEADER_NAMES = new String[]{"customerEmail", "orderNumber", "lineitems.variant.sku", "lineitems.price", "lineitems.quantity", "totalPrice"};

    @Bean
    public Job ordersImportJob(final Step ordersImportStep) {
        return jobBuilderFactory.get("ordersImportJob")
                .start(ordersImportStep)
                .build();
    }

    @Bean
    public Step ordersImportStep(final ItemReader<List<OrderCsvLineValue>> ordersImportReader,
                                 final ItemProcessor<List<OrderCsvLineValue>, OrderImportDraft> ordersImportProcessor,
                                 final ItemWriter<OrderImportDraft> ordersImportWriter) {
        return stepBuilderFactory.get("ordersImportStep")
                .<List<OrderCsvLineValue>, OrderImportDraft>chunk(1)
                .reader(ordersImportReader)
                .processor(ordersImportProcessor)
                .writer(ordersImportWriter)
                .build();
    }

    @Bean
    @StepScope
    public OrderImportItemReader ordersImportReader(@Value("#{jobParameters['resource']}") final Resource resource) {
        final OrderImportItemReader reader = new OrderImportItemReader();
        final SingleItemPeekableItemReader<OrderCsvLineValue> itemReader = new SingleItemPeekableItemReader<>();
        itemReader.setDelegate(flatFileItemReader(resource));
        reader.setItemReader(itemReader);
        return reader;
    }

    @Bean
    public ItemProcessor<List<OrderCsvLineValue>, OrderImportDraft> ordersImportProcessor() {
        return items -> {
            if (!items.isEmpty()) {
                final OrderCsvLineValue firstCsvLine = items.get(0);
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

    @Bean
    public ItemWriter<OrderImportDraft> ordersImportWriter(final BlockingSphereClient sphereClient) {
        return items -> items.stream()
                .map(OrderImportCommand::of)
                .forEach(sphereClient::executeBlocking);
    }

    private LineItemImportDraft lineItemToDraft(final OrderCsvLineValue item) {
        final ProductVariantImportDraft draft = ProductVariantImportDraftBuilder.ofSku(item.getLineItems().getVariant().getSku()).build();
        final long quantity = item.getLineItems().getQuantity();
        final Price price = Price.of(centAmountToMoney(item.getLineItems().getPrice()));
        final LocalizedString name = LocalizedString.ofEnglish("Product Name");
        return LineItemImportDraftBuilder.of(draft, quantity, price, name).build();
    }

    private static MonetaryAmount centAmountToMoney(final double centAmount) {
        return MoneyImpl.ofCents((long) (centAmount * 100), "EUR");
    }

    private static FlatFileItemReader<OrderCsvLineValue> flatFileItemReader(final Resource resource) {
        final FlatFileItemReader<OrderCsvLineValue> flatFileItemReader = new FlatFileItemReader<>();
        flatFileItemReader.setLineMapper(lineMapper());
        flatFileItemReader.setLinesToSkip(1);
        flatFileItemReader.setResource(resource);
        return flatFileItemReader;
    }

    private static DefaultLineMapper<OrderCsvLineValue> lineMapper() {
        return new DefaultLineMapper<OrderCsvLineValue>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(ORDER_CSV_HEADER_NAMES);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<OrderCsvLineValue>() {{
                setTargetType(OrderCsvLineValue.class);
            }});
        }};
    }
}
