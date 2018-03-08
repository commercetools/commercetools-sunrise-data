package com.commercetools.dataimport.products;

import com.commercetools.dataimport.LocalizedField;
import lombok.Data;

@Data
public class ProductCsvEntry {

    private String productType;
    private Long variantId;
    private String sku;
    private String prices;
    private String tax;
    private String categories;
    private String images;
    private LocalizedField name = new LocalizedField();
    private LocalizedField description = new LocalizedField();
    private LocalizedField slug = new LocalizedField();
}
