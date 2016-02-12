package com.commercetools.demo.dataimport.common;

import io.sphere.sdk.models.LocalizedString;

import java.util.Locale;

public class LocalizedField {
    private String de;
    private String en;
    private String it;

    public LocalizedField() {
    }

    public String getDe() {
        return de;
    }

    public void setDe(final String de) {
        this.de = de;
    }

    public String getEn() {
        return en;
    }

    public void setEn(final String en) {
        this.en = en;
    }

    public String getIt() {
        return it;
    }

    public void setIt(final String it) {
        this.it = it;
    }

    public LocalizedString toLocalizedString() {
        return LocalizedString.of(Locale.GERMAN, getDe()).plus(Locale.ENGLISH, getEn()).plus(Locale.ITALIAN, getIt());
    }
}
