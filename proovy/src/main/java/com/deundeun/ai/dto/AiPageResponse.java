package com.deundeun.ai.dto;

import java.util.List;

public record AiPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
    public static <T> AiPageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return new AiPageResponse<>(content, page, size, totalElements);
    }
}
