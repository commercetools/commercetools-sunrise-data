package com.commercetools.dataimport.orders;


/**
 * This class is used as a container for the csv line data
 */
public class OrderCsvLineValue {

    private String customerEmail;
    private String orderNumber;
    private LineItemCsvLineValue lineItems;
    private double totalPrice;

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(final String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LineItemCsvLineValue getLineItems() {
        return lineItems;
    }

    public void setLineItems(final LineItemCsvLineValue lineItems) {
        this.lineItems = lineItems;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(final double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
