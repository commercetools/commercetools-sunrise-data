package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.common.LocalizedField;

public class CategoryCsvLineValue {
    private LocalizedField name = new LocalizedField();
    private LocalizedField slug = new LocalizedField();
    private String parentId;
    private String externalId;
    private String webImageUrl;
    private String iosImageUrl;

    public CategoryCsvLineValue() {
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public LocalizedField getName() {
        return name;
    }

    public void setName(final LocalizedField name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public LocalizedField getSlug() {
        return slug;
    }

    public void setSlug(final LocalizedField slug) {
        this.slug = slug;
    }

    public String getWebImageUrl() {
        return webImageUrl;
    }

    public void setWebImageUrl(String webImageUrl) {
        this.webImageUrl = webImageUrl;
    }

    public String getIosImageUrl() {
        return iosImageUrl;
    }

    public void setIosImageUrl(String iosImageUrl) {
        this.iosImageUrl = iosImageUrl;
    }
}
