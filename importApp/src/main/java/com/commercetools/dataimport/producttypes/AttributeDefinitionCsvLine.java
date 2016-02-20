package com.commercetools.dataimport.producttypes;

import com.commercetools.dataimport.common.LocalizedField;
import io.sphere.sdk.models.Base;

public class AttributeDefinitionCsvLine extends Base {
    private String name;
    private String type;
    private String attributeConstraint;
    private String isRequired;
    private String isSearchable;
    private LocalizedField label = new LocalizedField();
    private String enumKey;
    private String enumLabel;
    private LocalizedField localizedEnumLabel = new LocalizedField();
    private String textInputHint;
    private String isVariant;

    public AttributeDefinitionCsvLine() {
    }

    public String getAttributeConstraint() {
        return attributeConstraint;
    }

    public void setAttributeConstraint(final String attributeConstraint) {
        this.attributeConstraint = attributeConstraint;
    }

    public String getEnumKey() {
        return enumKey;
    }

    public void setEnumKey(final String enumKey) {
        this.enumKey = enumKey;
    }

    public String getEnumLabel() {
        return enumLabel;
    }

    public void setEnumLabel(final String enumLabel) {
        this.enumLabel = enumLabel;
    }

    public String getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(final String isRequired) {
        this.isRequired = isRequired;
    }

    public String getIsSearchable() {
        return isSearchable;
    }

    public void setIsSearchable(final String isSearchable) {
        this.isSearchable = isSearchable;
    }

    public String getIsVariant() {
        return isVariant;
    }

    public void setIsVariant(final String isVariant) {
        this.isVariant = isVariant;
    }

    public LocalizedField getLabel() {
        return label;
    }

    public void setLabel(final LocalizedField label) {
        this.label = label;
    }

    public LocalizedField getLocalizedEnumLabel() {
        return localizedEnumLabel;
    }

    public void setLocalizedEnumLabel(final LocalizedField localizedEnumLabel) {
        this.localizedEnumLabel = localizedEnumLabel;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getTextInputHint() {
        return textInputHint;
    }

    public void setTextInputHint(final String textInputHint) {
        this.textInputHint = textInputHint;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
