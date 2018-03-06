package com.commercetools.dataimport.orders;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OrderImportItemReader extends AbstractItemStreamItemReader<List<OrderCsvEntry>> {

    private SingleItemPeekableItemReader<OrderCsvEntry> itemReader;

    @Override
    public void close() throws ItemStreamException {
        itemReader.close();
    }

    @Override
    public void open(final ExecutionContext executionContext) throws ItemStreamException {
        itemReader.open(executionContext);
    }

    @Override
    public void update(final ExecutionContext executionContext) throws ItemStreamException {
        itemReader.update(executionContext);
    }

    @Override
    public List<OrderCsvEntry> read() throws Exception {
        OrderCsvEntry csvLine = this.itemReader.peek();
        if (csvLine != null) {
            final List<OrderCsvEntry> orderLines = new ArrayList<>();
            final String orderId = orderId(csvLine);
            while (csvLine != null && orderId != null && orderId.equals(orderId(csvLine))) {
                orderLines.add(itemReader.read());
                csvLine = itemReader.peek();
            }
            return orderLines;
        }
        return null;
    }

    public void setItemReader(final SingleItemPeekableItemReader<OrderCsvEntry> itemReader){
        this.itemReader = itemReader;
    }

    @Nullable
    private static String orderId(@Nullable final OrderCsvEntry csvLine) {
        return csvLine != null && csvLine.getOrderNumber() != null ? csvLine.getOrderNumber().trim() : null;
    }
}
