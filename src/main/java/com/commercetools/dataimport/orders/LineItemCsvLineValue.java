package com.commercetools.dataimport.orders;


/**
 * This class is used as a container for the csv line data
 */
public class LineItemCsvLineValue {

    private double price;
    private long quantity;
    private VariantCsvLineValue variant;

    public double getPrice() {
        return price;
    }

    public void setPrice(final double price) {
        this.price = price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(final long quantity) {
        this.quantity = quantity;
    }

    public VariantCsvLineValue getVariant() {
        return variant;
    }

    public void setVariant(final VariantCsvLineValue variant) {
        this.variant = variant;
    }
}
