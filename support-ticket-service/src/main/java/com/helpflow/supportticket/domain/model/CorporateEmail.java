package com.helpflow.supportticket.domain.model;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Corporate e-mail rule: syntactically valid and not hosted by a public mailbox provider.
 */
public final class CorporateEmail {

    private static final Pattern FORMAT =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Set<String> PUBLIC_PROVIDERS = Set.of(
            "gmail.com", "googlemail.com", "hotmail.com", "outlook.com", "live.com", "yahoo.com",
            "yahoo.es", "icloud.com", "proton.me", "protonmail.com", "aol.com", "msn.com");

    private CorporateEmail() {
    }

    public static boolean isValid(String email) {
        if (email == null || email.length() > 254 || !FORMAT.matcher(email).matches()) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        return !PUBLIC_PROVIDERS.contains(domain);
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
