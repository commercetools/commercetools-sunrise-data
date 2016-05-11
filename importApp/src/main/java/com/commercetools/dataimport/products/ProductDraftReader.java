package com.commercetools.dataimport.products;

import com.commercetools.dataimport.common.LocalizedField;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.LocalizedStringEntry;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.attributes.*;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.search.SearchKeyword;
import io.sphere.sdk.search.SearchKeywords;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.utils.MoneyImpl;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static io.sphere.sdk.queries.QueryExecutionUtils.queryAll;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ProductDraftReader implements ItemStreamReader<ProductDraft> {
    private FlatFileItemReader<FieldSet> delegate;
    private final Resource attributeDefinitionsCsvResource;
    private final int maxProducts;
    private final BlockingSphereClient sphereClient;
    private int currentProducts = 0;
    private ProductDraftBuilder prevEntry = null;
    private String b2bCustomerGroupId;
    private List<ProductType> productTypes;
    private TaxCategory taxCategory;
    private CategoryTree categoryTree;
    private static final Pattern pricePattern = Pattern.compile("(?:(?<country>\\w{2})-)?(?<currency>\\w{3}) (?<centAmount>\\d{1,})(?:[|]\\d{1,})?(?:[ ](?<customerGroup>\\w\\p{Alnum}+))?$");

    public ProductDraftReader(final Resource attributeDefinitionsCsvResource, final int maxProducts, final BlockingSphereClient sphereClient) {
        this.attributeDefinitionsCsvResource = attributeDefinitionsCsvResource;
        this.maxProducts = maxProducts;
        this.sphereClient = sphereClient;
        final DefaultLineMapper<FieldSet> fullLineMapper = new DefaultLineMapper<FieldSet>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(new String[]{"productType","variantId","id","sku","prices","tax","categories","images","name.de","name.en","description.de","description.en","slug.de","slug.en","metaTitle.de","metaTitle.en","metaDescription.de","metaDescription.en","metaKeywords.de","metaKeywords.en","searchKeywords.de","searchKeywords.en","creationDate","articleNumberManufacturer","articleNumberMax","matrixId","baseId","designer","madeInItaly","completeTheLook","commonSize","size","color","colorFreeDefinition.de","colorFreeDefinition.en","details.de","details.en","style","gender","season","isOnStock","isLook","lookProducts","seasonNew"});
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
        if (currentProducts < maxProducts) {
            currentProducts++;
            return readDelegate();
        }
        return null;
    }

    private ProductDraft readDelegate() throws Exception {
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
        addImages(productsCsvEntry, builder);
        return builder;
    }

    private void addImages(final ProductsCsvEntry productsCsvEntry, final ProductVariantDraftBuilder builder) {
        if (productsCsvEntry.getImages().contains(";") || productsCsvEntry.getImages().contains(",")) {
            throw new RuntimeException("not prepared for multi images" + productsCsvEntry.getImages());
        }
        if (!isEmpty(productsCsvEntry.getImages())) {
            final String url = productsCsvEntry.getImages();
            builder.images(Image.of(url, ImageDimensions.of(0, 0)));
        }
    }

    private List<AttributeDraft> parseAttributes(final FieldSet currentLine, final List<ProductType> productTypes, final String productTypeKey) {
        final ProductType productType = productTypes.stream()
                .filter(p -> productTypeKey.equals(p.getKey()))
                .findFirst()
                .get();
        final Properties properties = currentLine.getProperties();
        final List<AttributeDraft> result = productType.getAttributes().stream()
                .map(a -> {
                    final AttributeType attributeType = a.getAttributeType();
                    final String name = a.getName();
                    if (attributeType instanceof DateTimeAttributeType || attributeType instanceof StringAttributeType || attributeType instanceof EnumAttributeType || attributeType instanceof LocalizedEnumAttributeType) {
                        final String value = properties.getProperty(name, null);
                        return isEmpty(value) ? null : AttributeDraft.of(name, value);
                    } else if(attributeType instanceof LocalizedStringAttributeType) {
                        final LocalizedString localizedString = createStreamOfLocalizedStringNames(currentLine, name)
                                .map(columnName -> {
                                    final String nullableValue = properties.getProperty(columnName);
                                    if (nullableValue == null) {
                                        return null;
                                    } else {
                                        return LocalizedStringEntry.of(columnName.replace(name + ".", ""), nullableValue);
                                    }
                                })
                                .filter(x -> x != null)
                                .collect(LocalizedString.streamCollector());
                        return AttributeDraft.of(name, localizedString);
                    } else if(attributeType instanceof SetAttributeType) {
                        final SetAttributeType setAttributeType = (SetAttributeType) attributeType;
                        final AttributeType elementType = setAttributeType.getElementType();
                        if (elementType instanceof StringAttributeType) {
                            final Set<String> values = Arrays.stream(properties.getProperty(name).split(";")).collect(toSet());
                            return AttributeDraft.of(name, values);
                        } else if (elementType instanceof LocalizedStringAttributeType) {
                            createStreamOfLocalizedStringNames(currentLine, name)
                                    .map(columnName -> {
                                        final String nullableValue = properties.getProperty(columnName);
                                        if (!isEmpty(nullableValue)) {
                                            throw new RuntimeException("not prepared for LocalizedStringAttributeType in set");
                                        } else {
                                            return null;
                                        }
                                    }).forEach(s -> {});
                            return null;
                        } else {
                            throw new RuntimeException("unknown element type of attribute type " + attributeType);
                        }
                    } else if(attributeType instanceof BooleanAttributeType) {
                        final String value = properties.getProperty(name);
                        return isEmpty(value) ? null : AttributeDraft.of(name, Boolean.valueOf(value));
                    } else {
                        throw new RuntimeException("unknown attribute type " + attributeType);
                    }
                })
                .filter(x -> x != null)
                .filter(x -> x.getValue() != null)
                .collect(toList());
        return result;
    }

    private Stream<String> createStreamOfLocalizedStringNames(final FieldSet currentLine, final String name) {
        return Arrays.asList(currentLine.getNames()).stream()
                .filter(columnName -> columnName.startsWith(name + "."));
    }

    private ProductDraftBuilder createNewEntry(final FieldSet currentLine) throws BindException {
        final ProductsCsvEntry productsCsvEntry = mapLineToEntry(currentLine);
        final ResourceIdentifier<ProductType> productType = ResourceIdentifier.ofKey(productsCsvEntry.getProductType());
        final LocalizedString name = productsCsvEntry.getName().toLocalizedString();
        final LocalizedString description = Optional.ofNullable(productsCsvEntry.getDescription())
                .map(LocalizedField::toLocalizedString).orElse(null);
        final LocalizedString slug = productsCsvEntry.getSlug().toLocalizedString();

        final String categories = productsCsvEntry.getCategories();

        final Set<Reference<Category>> categoriesSet;
        if (isNotEmpty(categories)) {
            final Stream<String> categoryPaths = Arrays.stream(categories.split(";"));//sth. like Women>Shoes>Loafers;Sale>Women>Shoes
            categoriesSet = categoryPaths.map(path -> {
                if (categoryTree == null) {
                    fillCache();
                }
                CategoryTree tree = categoryTree;
                Category foundCat = null;
                boolean continueFlag = true;
                for (final String catname : path.split(">")) {
                    //TODO handle category does not exist
                    if (continueFlag) {
                        foundCat = tree.getSubtreeRoots().stream()
                                .filter(cat -> catname.equals(cat.getName().get(Locale.ENGLISH)))
                                .findFirst()
                                .orElse(null);
                        continueFlag = foundCat != null;
                        if (continueFlag) {
                            tree = tree.getSubtree(tree.findChildren(foundCat));
                        }
                    }
                }
                return foundCat;
            })
            .filter(x -> x != null)
            .map(c -> c.toReference())
            .collect(toSet());
        } else {
            categoriesSet = Collections.emptySet();
        }

        final String pricesLine = productsCsvEntry.getPrices();
        final List<PriceDraft> prices = parsePricesLine(pricesLine);
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of()
                .sku(productsCsvEntry.getSku())
                .prices(prices)
                .attributes(parseAttributes(currentLine, productTypes, productsCsvEntry.getProductType()));
        addImages(productsCsvEntry, productVariantDraftBuilder);
        final ProductVariantDraft masterVariant = productVariantDraftBuilder
                .build();
        final SearchKeywords searchKeywords = searchKeywordsFromName(name);
        final ProductDraftBuilder entry = ProductDraftBuilder.of(productType, name, slug, masterVariant)
                .description(description)
                .taxCategory(taxCategory)
                .publish(true)
                .searchKeywords(searchKeywords)
                .categories(categoriesSet);
        return entry;
    }

    private SearchKeywords searchKeywordsFromName(final LocalizedString name) {
        final Map<Locale, List<SearchKeyword>> content = name.stream()
                .collect(toMap(entry -> entry.getLocale(),
                        entr -> Collections.singletonList(SearchKeyword.of(entr.getValue()))));
        return SearchKeywords.of(content);
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
            throw new RuntimeException(String.format("can't parse price for '%s'", priceString));
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
        fillCache();
    }

    private void fillCache() {
        b2bCustomerGroupId = sphereClient.executeBlocking(CustomerGroupQuery.of().byName("b2b")).head().get().getId();
        productTypes = blockingWait(queryAll(sphereClient, ProductTypeQuery.of()), 30, TimeUnit.SECONDS);
        final List<Category> categories = blockingWait(queryAll(sphereClient, CategoryQuery.of()), 3, TimeUnit.MINUTES);
        categoryTree = CategoryTree.of(categories);
        taxCategory = sphereClient.executeBlocking(TaxCategoryQuery.of().byName("standard")).head().get();
    }
}
