package com.commercetools.demo.dataimport.producttypes;

import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;

import java.util.LinkedList;
import java.util.List;

public class AttributeDefinitionCsvEntry extends AttributeDefinitionCsvLine {
    private List<EnumValue> enumValues = new LinkedList<>();
    private List<LocalizedEnumValue> localizedEnumValues = new LinkedList<>();

    public List<EnumValue> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(final List<EnumValue> enumValues) {
        this.enumValues = enumValues;
    }

    public List<LocalizedEnumValue> getLocalizedEnumValues() {
        return localizedEnumValues;
    }

    public void setLocalizedEnumValues(final List<LocalizedEnumValue> localizedEnumValues) {
        this.localizedEnumValues = localizedEnumValues;
    }
}
