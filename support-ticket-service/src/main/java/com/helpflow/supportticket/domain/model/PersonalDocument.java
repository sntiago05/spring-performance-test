package com.helpflow.supportticket.domain.model;

import java.util.Locale;

/**
 * Identity document rule: the same person must not be registered twice just because the document was
 * typed with other case or surrounding spaces ("ab-123" and " AB-123 " are the same document).
 */
public final class PersonalDocument {

    private PersonalDocument() {
    }

    public static String normalize(String document) {
        return document.trim().toUpperCase(Locale.ROOT);
    }
}
