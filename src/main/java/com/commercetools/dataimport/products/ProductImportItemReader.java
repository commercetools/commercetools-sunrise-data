package com.commercetools.dataimport.products;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

public class ProductImportItemReader extends AbstractItemStreamItemReader<List<FieldSet>> {

    private SingleItemPeekableItemReader<FieldSet> itemReader = new SingleItemPeekableItemReader<>();

    public ProductImportItemReader(final Resource resource, final String[] headers) {
        this.itemReader.setDelegate(createFlatFileItemReader(resource, headers));
    }

    private static FlatFileItemReader<FieldSet> createFlatFileItemReader(final Resource resource, final String[] headers) {
        final FlatFileItemReader<FieldSet> reader = new FlatFileItemReader<>();
        reader.setResource(resource);
        reader.setLineMapper(new DefaultLineMapper<FieldSet>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(headers);
                setStrict(false);
            }});
            setFieldSetMapper(new PassThroughFieldSetMapper());
        }});
        reader.setLinesToSkip(1);
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
    public List<FieldSet> read() throws Exception {
        FieldSet line = itemReader.read();
        if (line != null) {
            final List<FieldSet> lines = new ArrayList<>();
            lines.add(line);
            line = itemReader.peek();
            while (line != null && isVariantLine(line)) {
                lines.add(itemReader.read());
                line = itemReader.peek();
            }
            return lines;
        }
        return null;
    }

    private boolean isVariantLine(final FieldSet line) {
        final String productType = line.getProperties().getProperty("productType");
        return productType == null || productType.isEmpty();
    }
}
