package com.commercetools.dataimport.categories;

import lombok.Data;

@Data
public class CategoryCsvEntry {

    private LocalizedField name = new LocalizedField();
    private LocalizedField slug = new LocalizedField();
    private String parentId;
    private String externalId;
    private String webImageUrl;
    private String iosImageUrl;
}
