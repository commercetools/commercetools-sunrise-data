package com.commercetools.dataimport.orders;


/**
 * This class is used as a container for the csv line data
 */

public class OrderCsvLineValue {

    private String customerEmail;
    private String orderNumber;
    private LineItems lineItems;
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

    public LineItems getLineItems() {
        return lineItems;
    }

    public void setLineItems(LineItems lineItems) {
        this.lineItems = lineItems;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public static class LineItems {

        private Variant variant;
        private double price;
        private long quantity;

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }


        public Variant getVariant() {
            return variant;
        }

        public void setVariant(Variant variant) {
            this.variant = variant;
        }

        public static class Variant {

            private String sku;

            public Variant() {

            }

            public String getSku() {
                return sku;
            }

            public void setSku(String sku) {
                this.sku = sku;
            }

        }
    }

}
