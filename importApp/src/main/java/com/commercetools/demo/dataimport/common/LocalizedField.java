package com.commercetools.demo.dataimport.common;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.LocalizedStringEntry;

import java.util.Locale;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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
        return asList(LocalizedStringEntry.of(Locale.GERMAN, getDe()),
                LocalizedStringEntry.of(Locale.ENGLISH, getEn()),
                LocalizedStringEntry.of(Locale.ITALIAN, getIt()))
                .stream()
                .filter(entry -> !isEmpty(entry.getValue()))
                .collect(LocalizedString.streamCollector());
    }
}
