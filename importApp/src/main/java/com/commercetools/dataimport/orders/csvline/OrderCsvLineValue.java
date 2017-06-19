package com.commercetools.dataimport.orders.csvline;


/**
 * This class is used as a container for the csv line data
 */

public class OrderCsvLineValue {

    private String customerEmail;

    private String orderNumber;

    private LineItem lineItems;

    private double totalPrice;


    public OrderCsvLineValue() {
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LineItem getLineItems() {
        return lineItems;
    }

    public void setLineItems(LineItem lineItems) {
        this.lineItems = lineItems;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }



}
