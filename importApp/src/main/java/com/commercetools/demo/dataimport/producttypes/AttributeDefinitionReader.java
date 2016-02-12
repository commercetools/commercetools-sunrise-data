package com.commercetools.demo.dataimport.producttypes;

import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

class AttributeDefinitionReader implements ItemStreamReader<AttributeDefinitionCsvEntry> {
    private FlatFileItemReader<AttributeDefinitionCsvLine> delegate;
    private final Resource attributeDefinitionsCsvResource;
    private AttributeDefinitionCsvEntry prevEntry = null;

    public AttributeDefinitionReader(final Resource attributeDefinitionsCsvResource) {
        this.attributeDefinitionsCsvResource = attributeDefinitionsCsvResource;
        final DefaultLineMapper<AttributeDefinitionCsvLine> fullLineMapper = new DefaultLineMapper<AttributeDefinitionCsvLine>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(new String[]{"name", "type", "attributeConstraint", "isRequired", "isSearchable", "label.de", "label.en", "label.it", "enumKey", "enumLabel", "localizedEnumLabel.de", "localizedEnumLabel.en", "localizedEnumLabel.it", "textInputHint", "isVariant"});
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<AttributeDefinitionCsvLine>() {{
                setTargetType(AttributeDefinitionCsvLine.class);
            }});
        }};
        FlatFileItemReader<AttributeDefinitionCsvLine> reader = new FlatFileItemReader<>();
        reader.setResource(this.attributeDefinitionsCsvResource);
        reader.setLineMapper(fullLineMapper);
        reader.setLinesToSkip(1);
        this.delegate = reader;
    }

    @Override
    public AttributeDefinitionCsvEntry read() throws Exception, UnexpectedInputException, ParseException {
        AttributeDefinitionCsvEntry entry = null;
        AttributeDefinitionCsvLine currentLine = null;
        do {
            currentLine = delegate.read();
            if (currentLine != null) {
                if (isNewEntry(currentLine)) {
                    if (prevEntry != null) {
                        entry = prevEntry;
                        prevEntry = createNewEntry(currentLine);
                    } else {
                        prevEntry = createNewEntry(currentLine);
                    }
                } else {
                    final boolean isEnumDetailLine = !StringUtils.isEmpty(currentLine.getEnumLabel());
                    if (isEnumDetailLine) {
                        prevEntry.getEnumValues().add(EnumValue.of(currentLine.getEnumKey(), currentLine.getEnumLabel()));
                    } else {
                        final boolean isLocalizedEnumDetailLine = !StringUtils.isEmpty(currentLine.getLocalizedEnumLabel().getDe());
                        if (isLocalizedEnumDetailLine) {
                            prevEntry.getLocalizedEnumValues().add(LocalizedEnumValue.of(currentLine.getEnumKey(), currentLine.getLocalizedEnumLabel().toLocalizedString()));
                        } else {
                            throw new IllegalStateException("cannot map line " + currentLine + " for entry " + prevEntry);
                        }
                    }
                }
            }
        } while (currentLine != null && entry == null);
        return entry;
    }

    private AttributeDefinitionCsvEntry createNewEntry(final AttributeDefinitionCsvLine currentLine) {
        final AttributeDefinitionCsvEntry entry = new AttributeDefinitionCsvEntry();
        BeanUtils.copyProperties(currentLine, entry);
        return entry;
    }

    private boolean isNewEntry(final AttributeDefinitionCsvLine currentLine) {
        return !StringUtils.isEmpty(currentLine.getName());
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    @Override
    public void open(final ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(final ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }
}
