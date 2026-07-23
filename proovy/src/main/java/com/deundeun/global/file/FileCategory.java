package com.deundeun.global.file;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
public enum FileCategory {

    PROFILE("profile", 20L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp")),
    CERTIFICATION("certification", 20L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp")),
    CHALLENGE_THUMBNAIL("challenge", 20L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp")),
    CHAT("chat", 50L * 1024 * 1024, Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip"
    ));

    @Getter
    private final String directory;
    @Getter
    private final long maxFileSizeBytes;
    private final Set<String> allowedContentTypes;

    public boolean isAllowedContentType(String contentType) {
        return contentType != null && allowedContentTypes.contains(contentType);
    }
}
