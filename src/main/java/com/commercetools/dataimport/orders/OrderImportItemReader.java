package com.commercetools.dataimport.orders;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.core.io.Resource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OrderImportItemReader extends AbstractItemStreamItemReader<List<OrderCsvEntry>> {

    private SingleItemPeekableItemReader<OrderCsvEntry> itemReader = new SingleItemPeekableItemReader<>();

    public OrderImportItemReader(final Resource resource, final String[] headers) {
        itemReader.setDelegate(createFlatFileItemReader(resource, headers));
    }

    private static FlatFileItemReader<OrderCsvEntry> createFlatFileItemReader(final Resource resource, final String[] headers) {
        final FlatFileItemReader<OrderCsvEntry> reader = new FlatFileItemReader<>();
        reader.setLineMapper(new DefaultLineMapper<OrderCsvEntry>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(headers);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<OrderCsvEntry>() {{
                setTargetType(OrderCsvEntry.class);
            }});
        }});
        reader.setLinesToSkip(1);
        reader.setResource(resource);
        return reader;
    }

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
        OrderCsvEntry csvLine = itemReader.peek();
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

    @Nullable
    private static String orderId(@Nullable final OrderCsvEntry csvLine) {
        return csvLine != null && csvLine.getOrderNumber() != null ? csvLine.getOrderNumber().trim() : null;
    }
}
