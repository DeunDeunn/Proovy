package com.deundeun.global.file;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
public enum FileCategory {

    PROFILE("profile", 10L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp")),
    CERTIFICATION("certification", 10L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp")),
    CHAT("chat", 20L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp", "image/gif"));

    @Getter
    private final String directory;
    @Getter
    private final long maxFileSizeBytes;
    private final Set<String> allowedContentTypes;

    public boolean isAllowedContentType(String contentType) {
        return contentType != null && allowedContentTypes.contains(contentType);
    }
}
