package com.commercetools.demo.dataimport.products;

import com.commercetools.demo.dataimport.common.LocalizedField;

public class ProductsCsvEntry {
    private String productType;
    private String variantId;
    private String id;
    private String sku;
    private String prices;
    private String categories;
    private String images;
    private LocalizedField name = new LocalizedField();
    private LocalizedField description = new LocalizedField();
    private LocalizedField slug = new LocalizedField();

    public ProductsCsvEntry() {
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(final String categories) {
        this.categories = categories;
    }

    public LocalizedField getDescription() {
        return description;
    }

    public void setDescription(final LocalizedField description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public LocalizedField getName() {
        return name;
    }

    public void setName(final LocalizedField name) {
        this.name = name;
    }

    public String getPrices() {
        return prices;
    }

    public void setPrices(final String prices) {
        this.prices = prices;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(final String productType) {
        this.productType = productType;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(final String sku) {
        this.sku = sku;
    }

    public LocalizedField getSlug() {
        return slug;
    }

    public void setSlug(final LocalizedField slug) {
        this.slug = slug;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(final String variantId) {
        this.variantId = variantId;
    }

    public String getImages() {
        return images;
    }

    public void setImages(final String images) {
        this.images = images;
    }
}
