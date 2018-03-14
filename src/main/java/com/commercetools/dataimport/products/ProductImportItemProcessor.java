package com.commercetools.dataimport.products;

import com.commercetools.dataimport.CtpResourceRepository;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.models.*;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.attributes.*;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.utils.MoneyImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
public class ProductImportItemProcessor implements ItemProcessor<List<FieldSet>, ProductCreateCommand> {

    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:(?<country>\\w{2})-)?(?<currency>\\w{3}) (?<centAmount>\\d+)(?:\\|\\d+)?(?: (?<customerGroup>\\w+))?(?:#(?<channel>[\\w\\-]+))?$");

    private final CtpResourceRepository ctpResourceRepository;
    private CategoryTree categoryTree = null;

    public ProductImportItemProcessor(final CtpResourceRepository ctpResourceRepository) {
        this.ctpResourceRepository = ctpResourceRepository;
    }

    @Override
    public ProductCreateCommand process(final List<FieldSet> items) throws Exception {
        if (!items.isEmpty()) {
            final FieldSet productLine = items.get(0);
            final ProductCsvEntry productEntry = lineToCsvEntry(productLine);
            final ProductType productType = ctpResourceRepository.fetchProductType(productEntry.getProductType());
            if (productType != null) {
                final List<ProductVariantDraft> variantDrafts = variantLinesToDrafts(items, productType);
                final ProductDraft draft = productLineToDraft(productEntry, productType, variantDrafts);
                return ProductCreateCommand.of(draft);
            }
        }
        return null;
    }

    private ProductDraft productLineToDraft(final ProductCsvEntry productEntry, final ProductType productType,
                                            final List<ProductVariantDraft> variantDrafts) {
        final LocalizedString name = productEntry.getName().toLocalizedString();
        final LocalizedString slug = productEntry.getSlug().toLocalizedString();
        return ProductDraftBuilder.of(productType, name, slug, variantDrafts)
                .categories(parseCategories(productEntry))
                .taxCategory(parseTaxCategory(productEntry))
                .build();
    }

    private List<ProductVariantDraft> variantLinesToDrafts(final List<FieldSet> items, final ProductType productType) {
        return items.stream()
                .map(line -> {
                    try {
                        final ProductCsvEntry entry = lineToCsvEntry(line);
                        return variantLineToDraft(line, entry, productType);
                    } catch (BindException e) {
                        log.error("Could not parse product CSV entry", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private ProductVariantDraft variantLineToDraft(final FieldSet line, final ProductCsvEntry entry, final ProductType productType) {
        return ProductVariantDraftBuilder.of()
                .sku(entry.getSku())
                .prices(parsePrices(entry.getPrices()))
                .attributes(parseAttributes(line, productType))
                .images(parseImages(entry.getImages()))
                .build();
    }

    private ProductCsvEntry lineToCsvEntry(final FieldSet line) throws BindException {
        final FieldSetMapper<ProductCsvEntry> fieldSetMapper = new BeanWrapperFieldSetMapper<ProductCsvEntry>() {{
            setDistanceLimit(3);
            setTargetType(ProductCsvEntry.class);
            setStrict(false);
        }};
        return fieldSetMapper.mapFieldSet(line);
    }

    private Referenceable<TaxCategory> parseTaxCategory(final ProductCsvEntry entry) {
        final String taxCategoryId = ctpResourceRepository.fetchTaxCategoryId(entry.getTax());
        return taxCategoryId != null ? TaxCategory.referenceOfId(taxCategoryId) : null;
    }

    private Set<ResourceIdentifier<Category>> parseCategories(final ProductCsvEntry entry) {
        final String categories = entry.getCategories();
        if (isNotEmpty(categories)) {
            return Stream.of(categories.split(";"))
                    .map(this::findCategory)
                    .filter(Objects::nonNull)
                    .map(Category::toReference)
                    .collect(toSet());
        }
        return emptySet();
    }

    @Nullable
    private Category findCategory(final String path) {
        CategoryTree subtree = getCategoryTree();
        Category matchingCategory = null;
        for (final String categoryName : path.split(">")) {
            matchingCategory = subtree.getSubtreeRoots().stream()
                    .filter(category -> categoryName.equals(category.getName().get(ENGLISH)))
                    .findAny()
                    .orElse(null);
            if (matchingCategory == null) break;
            subtree = subtree.getSubtree(subtree.findChildren(matchingCategory));
        }
        return matchingCategory;
    }

    private CategoryTree getCategoryTree() {
        if (categoryTree == null) {
            categoryTree = ctpResourceRepository.fetchCategoryTree();
        }
        return categoryTree;
    }

    @Nullable
    private List<Image> parseImages(@Nullable final String images) {
        if (images != null && !images.isEmpty()) {
            return Stream.of(images.split(";"))
                    .filter(image -> !image.isEmpty())
                    .map(this::parseImage)
                    .collect(toList());
        }
        return null;
    }

    private Image parseImage(final String images) {
        return Image.of(images, ImageDimensions.of(0, 0));
    }

    @Nullable
    private List<PriceDraft> parsePrices(@Nullable final String prices) {
        if (prices != null && !prices.isEmpty()) {
            return Stream.of(prices.split(";"))
                    .filter(price -> !price.isEmpty())
                    .map(this::parsePrice)
                    .collect(toList());
        }
        return null;
    }

    private PriceDraft parsePrice(final String price) {
        final Matcher matcher = PRICE_PATTERN.matcher(price);
        if (!matcher.find()) {
            throw new RuntimeException("Cannot parse price: " + price);
        }
        final Long centAmount = Long.parseLong(matcher.group("centAmount"));
        final String currency = matcher.group("currency");
        return PriceDraftBuilder.of(MoneyImpl.ofCents(centAmount, currency))
                .country(extractCountry(matcher))
                .customerGroupId(extractCustomerGroupId(matcher))
                .channel(extractChannelRef(matcher))
                .build();
    }

    private CountryCode extractCountry(final Matcher matcher) {
        final String code = matcher.group("country");
        return code != null ? CountryCode.getByCode(code) : null;
    }

    private String extractCustomerGroupId(final Matcher matcher) {
        final String customerGroup = matcher.group("customerGroup");
        return customerGroup != null ? ctpResourceRepository.fetchCustomerGroupId(customerGroup) : null;
    }

    private Reference<Channel> extractChannelRef(final Matcher matcher) {
        final String channel = matcher.group("channel");
        return channel != null ? ctpResourceRepository.fetchChannelRef(channel) : null;
    }

    private List<AttributeDraft> parseAttributes(final FieldSet line, final ProductType productType) {
        return productType.getAttributes().stream()
                .map(attr -> parseAttribute(line, attr))
                .filter(draft -> draft != null && draft.getValue() != null)
                .collect(toList());
    }

    @Nullable
    private AttributeDraft parseAttribute(final FieldSet line, final AttributeDefinition attr) {
        final AttributeType attributeType = attr.getAttributeType();
        if (attributeType instanceof DateTimeAttributeType || attributeType instanceof StringAttributeType
                || attributeType instanceof EnumAttributeType || attributeType instanceof LocalizedEnumAttributeType
                || attributeType instanceof BooleanAttributeType) {
            return extractStringLikeAttributeDraft(line, attr.getName());
        } else if(attributeType instanceof LocalizedStringAttributeType) {
            return extractLocalizedStringAttributeDraft(line, attr.getName());
        } else if (attributeType instanceof SetAttributeType) {
            return extractSetAttributeDraft(line, (SetAttributeType) attributeType, attr.getName());
        } else {
            throw new RuntimeException("Not supported attribute type " + attributeType);
        }
    }

    private AttributeDraft extractSetAttributeDraft(final FieldSet line, final SetAttributeType attributeType, final String name) {
        final AttributeType elementType = attributeType.getElementType();
        final Properties properties = line.getProperties();
        if (elementType instanceof StringAttributeType) {
            final Set<String> values = Stream.of(properties.getProperty(name).split(";")).collect(toSet());
            return AttributeDraft.of(name, values);
        } else {
            throw new UnsupportedOperationException("Not supported element type of attribute type " + attributeType + " for field " + name);
        }
    }

    @Nullable
    private AttributeDraft extractLocalizedStringAttributeDraft(final FieldSet line, final String attrName) {
        final LocalizedString localizedString = streamOfLocalizedFields(line, attrName)
                .map(columnName -> {
                    final String value = line.getProperties().getProperty(columnName);
                    final String locale = columnName.replace(attrName + ".", "");
                    return LocalizedStringEntry.of(locale, value);
                })
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .collect(LocalizedString.streamCollector());
        return localizedString.getLocales().isEmpty() ? null : AttributeDraft.of(attrName, localizedString);
    }

    @Nullable
    private AttributeDraft extractStringLikeAttributeDraft(final FieldSet line, final String name) {
        final String value = line.getProperties().getProperty(name);
        return value != null && !value.isEmpty() ? AttributeDraft.of(name, value) : null;
    }

    private Stream<String> streamOfLocalizedFields(final FieldSet line, final String attrName) {
        return Stream.of(line.getNames()).filter(columnName -> columnName.startsWith(attrName + "."));
    }
}
