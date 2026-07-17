package com.deundeun.chat.dto.response;

import java.time.LocalDateTime;

public record ChatMessageDeletedEvent(
    String eventType,
    Long messageId,
    Long chatRoomId,
    LocalDateTime deletedAt
) {
    private static final String EVENT_TYPE = "MESSAGE_DELETED";

    public static ChatMessageDeletedEvent of(ChatMessageDeleteResponse response) {
        return new ChatMessageDeletedEvent(EVENT_TYPE, response.messageId(), response.chatRoomId(), response.deletedAt());
    }
}
