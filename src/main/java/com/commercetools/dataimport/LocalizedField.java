package com.commercetools.dataimport;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.LocalizedStringEntry;
import lombok.Data;

import java.util.Locale;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Data
public class LocalizedField {

    private String de;
    private String en;
    private String it;

    public LocalizedString toLocalizedString() {
        return Stream.of(
                LocalizedStringEntry.of(Locale.GERMAN, getDe()),
                LocalizedStringEntry.of(Locale.ENGLISH, getEn()),
                LocalizedStringEntry.of(Locale.ITALIAN, getIt()))
                .filter(entry -> !isEmpty(entry.getValue()))
                .collect(LocalizedString.streamCollector());
    }
}
