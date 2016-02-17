package com.commercetools.demo.dataimport.producttypes;

import java.util.LinkedList;
import java.util.List;

public class ProductTypeCsvEntry {
    private String name;
    private String description;
    private List<String> attributeNames = new LinkedList<>();

    public ProductTypeCsvEntry() {
    }

    public List<String> getAttributeNames() {
        return attributeNames;
    }

    public void setAttributeNames(final List<String> attributeNames) {
        this.attributeNames = attributeNames;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void addAttribute(final String attribute) {
        attributeNames.add(attribute);
    }
}
