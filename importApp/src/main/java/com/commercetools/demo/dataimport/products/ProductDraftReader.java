package com.commercetools.demo.dataimport.products;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;

import static java.util.Arrays.asList;

class ProductDraftReader implements ItemStreamReader<ProductDraft> {
    private FlatFileItemReader<FieldSet> delegate;
    private final Resource attributeDefinitionsCsvResource;
    private ProductDraftBuilder prevEntry = null;

    public ProductDraftReader(final Resource attributeDefinitionsCsvResource) {
        this.attributeDefinitionsCsvResource = attributeDefinitionsCsvResource;
        final DefaultLineMapper<FieldSet> fullLineMapper = new DefaultLineMapper<FieldSet>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(new String[]{"productType", "variantId", "id", "sku", "prices", "categories", "name.de", "description.de", "slug.de", "name.en", "description.en", "slug.en", "name.it", "description.it", "slug.it", "articleNumberManufacturer", "articleNumberMax", "matrixId", "baseId", "designer", "madeInItaly", "completeTheLook", "commonSize", "size", "color", "colorFreeDefinition.en", "colorFreeDefinition.de", "colorFreeDefinition.it", "details.en", "details.de", "details.it", "style", "gender", "season", "isOnStock", "isLook", "lookProducts", "seasonNew"});
                setStrict(false);
            }});
            setFieldSetMapper(new PassThroughFieldSetMapper());
        }};
        FlatFileItemReader<FieldSet> reader = new FlatFileItemReader<>();
        reader.setResource(this.attributeDefinitionsCsvResource);
        reader.setLineMapper(fullLineMapper);
        reader.setLinesToSkip(1);
        this.delegate = reader;
    }

    @Override
    public ProductDraft read() throws Exception {
        ProductDraftBuilder entry = null;
        FieldSet currentLine = null;
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
                    //TODO
                    //add to prevEntry the variants
                }
            }
        } while (currentLine != null && entry == null);
        return entry != null ? entry.build() : null;
    }

    private ProductDraftBuilder createNewEntry(final FieldSet currentLine) throws BindException {
        final FieldSetMapper<ProductsCsvEntry> fieldSetMapper = new BeanWrapperFieldSetMapper<ProductsCsvEntry>() {{
            setDistanceLimit(3);
            setTargetType(ProductsCsvEntry.class);
            setStrict(false);
        }};
        final ProductsCsvEntry productsCsvEntry = fieldSetMapper.mapFieldSet(currentLine);

        final ResourceIdentifier<ProductType> productType = ResourceIdentifier.ofKey(productsCsvEntry.getProductType());
        final LocalizedString name = productsCsvEntry.getName().toLocalizedString();
        final LocalizedString slug = productsCsvEntry.getSlug().toLocalizedString();

        asList(productsCsvEntry.getPrices().split(";")).forEach(System.err::println);

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of()
                .build();
        final ProductDraftBuilder entry = ProductDraftBuilder.of(productType, name, slug, masterVariant);
        return entry;
    }

    private boolean isNewEntry(final FieldSet currentLine) {
        final String productTypeKey = currentLine.getValues()[0];
        return !StringUtils.isEmpty(productTypeKey);
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
