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

    private static final String[] PRODUCT_CSV_HEADER_NAMES = {"productType", "variantId", "sku", "prices", "tax", "categories", "images", "name.de", "name.en", "description.de", "description.en", "slug.de", "slug.en", "metaTitle.de", "metaTitle.en", "metaDescription.de", "metaDescription.en", "metaKeywords.de", "metaKeywords.en", "searchKeywords.de", "searchKeywords.en", "creationDate", "articleNumberManufacturer", "articleNumberMax", "matrixId", "baseId", "designer", "madeInItaly", "completeTheLook", "commonSize", "size", "color", "colorFreeDefinition.de", "colorFreeDefinition.en", "details.de", "details.en", "style", "gender", "season", "isOnStock", "isLook", "lookProducts", "seasonNew"};

    private SingleItemPeekableItemReader<FieldSet> itemReader = new SingleItemPeekableItemReader<>();

    public ProductImportItemReader(final Resource resource) {
        itemReader.setDelegate(createFlatFileItemReader(resource));
    }

    private static FlatFileItemReader<FieldSet> createFlatFileItemReader(final Resource resource) {
        final FlatFileItemReader<FieldSet> reader = new FlatFileItemReader<>();
        reader.setLineMapper(new DefaultLineMapper<FieldSet>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(PRODUCT_CSV_HEADER_NAMES);
                setStrict(false);
            }});
            setFieldSetMapper(new PassThroughFieldSetMapper());
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
    public List<FieldSet> read() throws Exception {
        FieldSet line = itemReader.read();
        if (line != null) {
            final List<FieldSet> lines = new ArrayList<>();
            lines.add(line);
            line = itemReader.peek();
            while (line != null && !isProductLine(line)) {
                lines.add(itemReader.read());
                line = itemReader.peek();
            }
            return lines;
        }
        return null;
    }

    private boolean isProductLine(final FieldSet line) {
        return line.getProperties().getProperty("productType") != null;
    }
}
