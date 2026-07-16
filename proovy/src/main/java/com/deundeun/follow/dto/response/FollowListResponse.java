package com.deundeun.follow.dto.response;

import com.deundeun.follow.dto.FollowUserItem;

import java.util.List;

public record FollowListResponse(
    List<FollowUserItem> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

    public static FollowListResponse of(List<FollowUserItem> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = (long) (page + 1) * size < totalElements;

        return new FollowListResponse(content, page, size, totalElements, totalPages, hasNext);
    }
}
