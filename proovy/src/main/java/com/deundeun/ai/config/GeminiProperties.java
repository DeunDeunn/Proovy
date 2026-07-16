package com.deundeun.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        Api api,
        String model
) {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    public String apiKey() {
        return api == null ? null : api.key();
    }

    public String apiBaseUrl() {
        if (api == null || isBlank(api.baseUrl())) {
            return DEFAULT_BASE_URL;
        }
        return api.baseUrl();
    }

    public String modelName() {
        if (isBlank(model)) {
            return DEFAULT_MODEL;
        }
        return model.toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Api(
            String key,
            String baseUrl
    ) {
    }
}
