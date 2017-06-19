package com.commercetools.dataimport.orders;

import com.commercetools.dataimport.orders.csvline.OrderCsvLineValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.orders.*;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.utils.MoneyImpl;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class OrderImportItemReader implements ItemReader<OrderImportDraft>, ItemStream {


    private SingleItemPeekableItemReader<OrderCsvLineValue> singleItemPeekableItemReader;

    public OrderImportItemReader() {}

    public void setDelegate(final SingleItemPeekableItemReader singleItemPeekableItemReader){
        this.singleItemPeekableItemReader = singleItemPeekableItemReader;
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

        OrderCsvLineValue nextLine = this.singleItemPeekableItemReader.peek();

        if (nextLine == null) {
            return null;
        }

        final String currentID = getOrderIdUsingEmailAndOrderNumber(nextLine);

        OrderImportDraftBuilder orderImportDraftBuilder = OrderImportDraftBuilder.ofLineItems(
                MoneyImpl.ofCents((long) (nextLine.getTotalPrice() * 100), "EUR"),
                OrderState.COMPLETE,
                null)
                .customerEmail(nextLine.getCustomerEmail())
                .orderNumber(nextLine.getOrderNumber());

        List<LineItemImportDraft> lineItems = new ArrayList<>();

        while (nextLine != null && currentID.equals(getOrderIdUsingEmailAndOrderNumber(nextLine))) {
            nextLine = singleItemPeekableItemReader.read();
            lineItems.add(extractLineItemDraftFromCSVLine(nextLine));
            nextLine = singleItemPeekableItemReader.peek();
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
