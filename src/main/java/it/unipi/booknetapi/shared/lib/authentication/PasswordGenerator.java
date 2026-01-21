package it.unipi.booknetapi.shared.lib.authentication;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String PUNCTUATION = "!@#$%&*()_+-=[]|,./?><";

    private final boolean useLower;
    private final boolean useUpper;
    private final boolean useDigits;
    private final boolean usePunctuation;

    // Use SecureRandom for cryptographically strong random number generation
    private final SecureRandom random = new SecureRandom();

    private PasswordGenerator(PasswordGeneratorBuilder builder) {
        this.useLower = builder.useLower;
        this.useUpper = builder.useUpper;
        this.useDigits = builder.useDigits;
        this.usePunctuation = builder.usePunctuation;
    }

    public static class PasswordGeneratorBuilder {
        private boolean useLower;
        private boolean useUpper;
        private boolean useDigits;
        private boolean usePunctuation;

        public PasswordGeneratorBuilder() {
            this.useLower = false;
            this.useUpper = false;
            this.useDigits = false;
            this.usePunctuation = false;
        }

        public PasswordGeneratorBuilder useLower(boolean useLower) {
            this.useLower = useLower;
            return this;
        }

        public PasswordGeneratorBuilder useUpper(boolean useUpper) {
            this.useUpper = useUpper;
            return this;
        }

        public PasswordGeneratorBuilder useDigits(boolean useDigits) {
            this.useDigits = useDigits;
            return this;
        }

        public PasswordGeneratorBuilder usePunctuation(boolean usePunctuation) {
            this.usePunctuation = usePunctuation;
            return this;
        }

        public PasswordGenerator build() {
            return new PasswordGenerator(this);
        }
    }

    public String generate(int length) {
        if (length <= 0) {
            return "";
        }

        // 1. Build the list of characters to choose from
        StringBuilder charCategories = new StringBuilder();
        List<String> passwordChars = new ArrayList<>(length);

        // 2. Ensure at least one character from each selected category is included
        if (useLower) {
            charCategories.append(LOWER);
            passwordChars.add(String.valueOf(LOWER.charAt(random.nextInt(LOWER.length()))));
        }
        if (useUpper) {
            charCategories.append(UPPER);
            passwordChars.add(String.valueOf(UPPER.charAt(random.nextInt(UPPER.length()))));
        }
        if (useDigits) {
            charCategories.append(DIGITS);
            passwordChars.add(String.valueOf(DIGITS.charAt(random.nextInt(DIGITS.length()))));
        }
        if (usePunctuation) {
            charCategories.append(PUNCTUATION);
            passwordChars.add(String.valueOf(PUNCTUATION.charAt(random.nextInt(PUNCTUATION.length()))));
        }

        // 3. Fill the remaining length with random characters from all selected categories
        String charPool = charCategories.toString();
        if (charPool.isEmpty()) {
            throw new IllegalArgumentException("At least one character category must be selected!");
        }

        for (int i = passwordChars.size(); i < length; i++) {
            passwordChars.add(String.valueOf(charPool.charAt(random.nextInt(charPool.length()))));
        }

        // 4. Shuffle the result to prevent the "guaranteed" characters from always appearing at the start
        Collections.shuffle(passwordChars, random);

        return String.join("", passwordChars);
    }
}
