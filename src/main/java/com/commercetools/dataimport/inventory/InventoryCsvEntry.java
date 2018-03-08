package com.commercetools.dataimport.inventory;

import lombok.Data;

@Data
public class InventoryCsvEntry {

    private String sku;
    private Long quantityOnStock;
    private String supplyChannel;
}
