package com.deundeun.chat.dto.response;

import java.util.List;

public record ChatMessageListResponse(
    List<ChatMessageResponse> content,
    int size,
    boolean hasNext,
    Long nextCursor
) {

    public static ChatMessageListResponse of(List<ChatMessageResponse> content, int size, boolean hasNext, Long nextCursor) {
        return new ChatMessageListResponse(content, size, hasNext, nextCursor);
    }
}
