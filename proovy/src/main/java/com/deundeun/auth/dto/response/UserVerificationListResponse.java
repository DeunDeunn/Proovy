package com.deundeun.auth.dto.response;

import com.deundeun.auth.dto.UserVerificationListItem;

import java.util.List;

public record UserVerificationListResponse(
    List<UserVerificationListItem> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

    public static UserVerificationListResponse of(List<UserVerificationListItem> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = (long) (page + 1) * size < totalElements;

        return new UserVerificationListResponse(content, page, size, totalElements, totalPages, hasNext);
    }
}
