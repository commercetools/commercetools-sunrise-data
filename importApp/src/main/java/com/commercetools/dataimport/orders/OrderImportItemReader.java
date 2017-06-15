package com.commercetools.dataimport.orders;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.orders.*;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.utils.MoneyImpl;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class OrderImportItemReader implements ItemReader<OrderImportDraft>, ItemStream {

    public static final String[] ORDER_CSV_HEADER_NAMES = new String[]{"customerEmail", "orderNumber", "lineitems.variant.sku", "lineitems.price", "lineitems.quantity", "totalPrice"};

    private final SingleItemPeekableItemReader<OrderCsvLineValue> singleItemPeekableItemReader;

    private final FlatFileItemReader flatFileItemReader;


    public OrderImportItemReader() {

        flatFileItemReader = new FlatFileItemReader<>();
        flatFileItemReader.setLineMapper(new DefaultLineMapper<OrderCsvLineValue>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(ORDER_CSV_HEADER_NAMES);

            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<OrderCsvLineValue>() {{
                setTargetType(OrderCsvLineValue.class);
            }});
        }});
        flatFileItemReader.setLinesToSkip(1);

        singleItemPeekableItemReader = new SingleItemPeekableItemReader<>();
        singleItemPeekableItemReader.setDelegate(flatFileItemReader);

    }

    public void setResource(Resource resource) {
        this.flatFileItemReader.setResource(resource);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        singleItemPeekableItemReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        singleItemPeekableItemReader.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        singleItemPeekableItemReader.close();
    }

    @Override
    public OrderImportDraft read() throws Exception {

        OrderCsvLineValue currentLine = this.singleItemPeekableItemReader.read();

        if (currentLine == null) {
            return null;
        }

        final String currentID = getOrderIdUsingEmailAndOrderNumber(currentLine);

        OrderImportDraftBuilder orderImportDraftBuilder = OrderImportDraftBuilder.ofLineItems(
                MoneyImpl.ofCents((long) (currentLine.getTotalPrice() * 100), "EUR"),
                OrderState.COMPLETE,
                null)
                .customerEmail(currentLine.getCustomerEmail())
                .orderNumber(currentLine.getOrderNumber());

        List<LineItemImportDraft> lineItems = new ArrayList<>();
        lineItems.add(extractLineItemDraftFromCSVLine(currentLine));
        OrderCsvLineValue next = singleItemPeekableItemReader.peek();

        while (next != null && currentID.equals(getOrderIdUsingEmailAndOrderNumber(next))) {
            currentLine = singleItemPeekableItemReader.read();
            lineItems.add(extractLineItemDraftFromCSVLine(currentLine));
            next = singleItemPeekableItemReader.peek();
        }

        orderImportDraftBuilder.lineItems(lineItems);
        return orderImportDraftBuilder.build();
    }


    private static String getOrderIdUsingEmailAndOrderNumber(OrderCsvLineValue orderCsvLineValue) {

        return orderCsvLineValue.getCustomerEmail().trim() + "_" + orderCsvLineValue.getOrderNumber().trim();

    }


    private static LineItemImportDraft extractLineItemDraftFromCSVLine(OrderCsvLineValue readLine) {

        ProductVariantImportDraftBuilder productVariantImportDraftBuilder = ProductVariantImportDraftBuilder.
                ofSku(readLine.getLineItems().getVariant().getSku());

        LineItemImportDraftBuilder lineItemImportDraftBuilder = LineItemImportDraftBuilder.of(productVariantImportDraftBuilder.build(),
                readLine.getLineItems().getQuantity(),
                Price.of(MoneyImpl.ofCents((long) (readLine.getLineItems().getPrice() * 100), "EUR")),
                en("Product Name"));

        return lineItemImportDraftBuilder.build();

    }

    public static LocalizedString en(final String value) {
        return LocalizedString.of(Locale.ENGLISH, value);
    }
}
