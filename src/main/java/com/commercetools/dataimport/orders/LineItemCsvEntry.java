package com.commercetools.dataimport.orders;


import lombok.Data;

/**
 * This class is used as a container for the csv line data
 */
@Data
public class LineItemCsvEntry {

    private double price;
    private long quantity;
    private VariantCsvEntry variant;
}
