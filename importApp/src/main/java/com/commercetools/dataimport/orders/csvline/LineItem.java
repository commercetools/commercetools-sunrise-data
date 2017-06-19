package com.commercetools.dataimport.orders.csvline;

public  class LineItem {

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

    }