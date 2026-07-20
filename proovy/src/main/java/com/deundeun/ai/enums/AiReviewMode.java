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
        if ("AUTO_DECISION".equals(normalized)) {
            return AUTO.name();
        }
        if ("ASSIST_ONLY".equals(normalized)) {
            return MANUAL.name();
        }

        AiReviewMode.valueOf(normalized);
        return normalized;
    }
}
