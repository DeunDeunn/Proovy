package com.deundeun.ai.enums;

import java.util.Locale;

public enum AiReviewMode {
    AUTO,
    MANUAL;

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        AiReviewMode.valueOf(normalized);
        return normalized;
    }
}
