package com.deundeun.chat.dto.response;

import java.util.List;

public record ChatRoomListResponse(
    List<ChatRoomSummaryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

    public static ChatRoomListResponse of(List<ChatRoomSummaryResponse> content,
                                          int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = (long) (page + 1) * size < totalElements;

        return new ChatRoomListResponse(content, page, size, totalElements, totalPages, hasNext);
    }
}
