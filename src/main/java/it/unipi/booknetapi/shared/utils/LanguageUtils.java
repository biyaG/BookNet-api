package it.unipi.booknetapi.shared.utils;

import java.util.Locale;
import java.util.Optional;

public class LanguageUtils {

    /**
     * Returns the ISO 639 language code for a given ISO 3166 country code.
     * * @param countryCode The 2-letter country code (e.g., "US", "FR", "IT")
     * @return Optional containing "en", "fr", "it" etc. (ISO 639-1), or empty if unknown.
     */
    public static Optional<String> getLanguageFromCountry(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return Optional.empty();
        }

        for (Locale available : Locale.getAvailableLocales()) {
            if (available.getCountry().equalsIgnoreCase(countryCode)) {
                return Optional.of(available.getLanguage());
            }
        }

        return Optional.empty();
    }
}
