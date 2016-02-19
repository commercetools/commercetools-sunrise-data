package com.commercetools.demo.dataimport.products;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.LocalizedStringEntry;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.attributes.*;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeLocalRepository;
import io.sphere.sdk.utils.MoneyImpl;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

class ProductDraftReader implements ItemStreamReader<ProductDraft> {
    private FlatFileItemReader<FieldSet> delegate;
    private final Resource attributeDefinitionsCsvResource;
    private ProductDraftBuilder prevEntry = null;
    private String b2bCustomerGroupId;
    private List<ProductType> productTypes;
    private static final Pattern pricePattern = Pattern.compile("(?:(?<country>\\w{2})-)?(?<currency>\\w{3}) (?<centAmount>\\d{1,})(?:[ ](?<customerGroup>\\p{Alnum}+))?");

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
                    final ProductVariantDraftBuilder variantDraftBuilder = createNewVariantDraftBuilder(currentLine, prevEntry.build().getProductType().getKey());
                    prevEntry.plusVariants(variantDraftBuilder.build());
                }
            }
        } while (currentLine != null && entry == null);
        return entry != null ? entry.build() : null;
    }

    private ProductVariantDraftBuilder createNewVariantDraftBuilder(final FieldSet currentLine, final String productTypeKey) throws BindException {
        final ProductsCsvEntry productsCsvEntry = mapLineToEntry(currentLine);
        final ProductVariantDraftBuilder builder = ProductVariantDraftBuilder.of();
        builder.sku(productsCsvEntry.getSku());
        builder.prices(parsePricesLine(productsCsvEntry.getPrices()));
        builder.attributes(parseAttributes(currentLine, productTypes, productTypeKey));
        return builder;
    }

    private List<AttributeDraft> parseAttributes(final FieldSet currentLine, final List<ProductType> productTypes, final String productTypeKey) {
        final ProductType productType = productTypes.stream().filter(p -> p.getKey().equals(productTypeKey)).findFirst().get();
        final Properties properties = currentLine.getProperties();
        final List<AttributeDraft> result = productType.getAttributes().stream()
                .map(a -> {
                    final AttributeType attributeType = a.getAttributeType();
                    final String name = a.getName();
                    if (attributeType instanceof DateTimeAttributeType || attributeType instanceof StringAttributeType || attributeType instanceof EnumAttributeType || attributeType instanceof LocalizedEnumAttributeType) {
                        final String value = properties.getProperty(name, null);
                        return AttributeDraft.of(name, value);
                    } else if(attributeType instanceof LocalizedStringAttributeType) {
                        final LocalizedString localizedString = Arrays.asList(currentLine.getNames()).stream()
                                .filter(columnName -> columnName.startsWith(name + "."))
                                .map(columnName -> {
                                    final String nullableValue = properties.getProperty(columnName);
                                    if (nullableValue == null) {
                                        return null;
                                    } else {
                                        final Locale locale = Locale.forLanguageTag(columnName.replace(name + ".", ""));
                                        return LocalizedStringEntry.of(locale, nullableValue);
                                    }
                                })
                                .filter(x -> x != null)
                                .collect(LocalizedString.streamCollector());
                        return AttributeDraft.of(name, localizedString);
                    } else {
                        return null;
                    }
                })
                .filter(x -> x != null)
                .filter(x -> x.getValue() != null)
                .collect(toList());
        return result;
    }

    private ProductDraftBuilder createNewEntry(final FieldSet currentLine) throws BindException {
        final ProductsCsvEntry productsCsvEntry = mapLineToEntry(currentLine);
        final ResourceIdentifier<ProductType> productType = ResourceIdentifier.ofKey(productsCsvEntry.getProductType());
        final LocalizedString name = productsCsvEntry.getName().toLocalizedString();
        final LocalizedString slug = productsCsvEntry.getSlug().toLocalizedString();

        final String pricesLine = productsCsvEntry.getPrices();
        final List<PriceDraft> prices = parsePricesLine(pricesLine);
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of()
                .prices(prices)
                .attributes(parseAttributes(currentLine, productTypes, productsCsvEntry.getProductType()))
                .build();
        final ProductDraftBuilder entry = ProductDraftBuilder.of(productType, name, slug, masterVariant);
        return entry;
    }

    private List<PriceDraft> parsePricesLine(final String pricesLine) {
        return asList(pricesLine.split(";"))
                    .stream()
                    .filter(s -> !StringUtils.isEmpty(s))
                    .map((String priceString) -> parsePriceString(priceString))
                    .collect(toList());
    }

    private ProductsCsvEntry mapLineToEntry(final FieldSet currentLine) throws BindException {
        final FieldSetMapper<ProductsCsvEntry> fieldSetMapper = getProductsCsvEntryFieldSetMapper();
        return fieldSetMapper.mapFieldSet(currentLine);
    }

    private FieldSetMapper<ProductsCsvEntry> getProductsCsvEntryFieldSetMapper() {
        return new BeanWrapperFieldSetMapper<ProductsCsvEntry>() {{
                setDistanceLimit(3);
                setTargetType(ProductsCsvEntry.class);
                setStrict(false);
            }};
    }

    private PriceDraft parsePriceString(final String priceString) {
        final Matcher matcher = pricePattern.matcher(priceString);
        if (!matcher.find()) {
            throw new RuntimeException("can't parse price for " + priceString);
        }
        final String currencyCode = matcher.group("currency");
        final String centAmount = matcher.group("centAmount");
        final String nullableCountryCode = matcher.group("country");
        final String nullableCustomerGroup = matcher.group("customerGroup");

        final Reference<CustomerGroup> customerGroup =
                (!isEmpty(nullableCustomerGroup) && "b2b".equals(nullableCustomerGroup))
                ? b2bCustomerGroupReference()
                : null;
        return PriceDraft.of(MoneyImpl.ofCents(Long.parseLong(centAmount), currencyCode))
                .withCountry(nullableCountryCode == null ? null : CountryCode.valueOf(nullableCountryCode))
                .withCustomerGroup(customerGroup);
    }

    private Reference<CustomerGroup> b2bCustomerGroupReference() {
        return CustomerGroup.referenceOfId(b2bCustomerGroupId);
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

    @BeforeStep
    public void retrieveInterStepData(final StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        b2bCustomerGroupId = (String) jobContext.get(ProductsImportJobConfiguration.b2bCustomerGroupStepContextKey);
        productTypes = (List<ProductType>) jobContext.get(ProductsImportJobConfiguration.productTypesStepContextKey);
    }
}
