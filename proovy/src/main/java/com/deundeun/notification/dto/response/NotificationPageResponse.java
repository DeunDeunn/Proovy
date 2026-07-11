package com.deundeun.notification.dto.response;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

    public static NotificationPageResponse of(List<NotificationResponse> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = (long) (page + 1) * size < totalElements;

        return new NotificationPageResponse(content, page, size, totalElements, totalPages, hasNext);
    }
}
