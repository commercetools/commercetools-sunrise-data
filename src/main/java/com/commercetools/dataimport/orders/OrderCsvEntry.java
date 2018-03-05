package com.commercetools.dataimport.orders;


import lombok.Data;

/**
 * This class is used as a container for the csv line data
 */
@Data
public class OrderCsvEntry {

    private String customerEmail;
    private String orderNumber;
    private LineItemCsvEntry lineItems;
    private double totalPrice;
}
