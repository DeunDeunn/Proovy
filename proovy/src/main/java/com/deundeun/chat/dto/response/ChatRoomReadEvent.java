package com.deundeun.chat.dto.response;

import java.time.LocalDateTime;

public record ChatRoomReadEvent(
    String eventType,
    Long chatRoomId,
    Long userId,
    Long lastReadMessageId,
    LocalDateTime lastReadAt
) {
    private static final String EVENT_TYPE = "ROOM_READ";

    public static ChatRoomReadEvent of(ChatRoomReadResponse response) {
        return new ChatRoomReadEvent(
            EVENT_TYPE,
            response.chatRoomId(),
            response.userId(),
            response.lastReadMessageId(),
            response.lastReadAt()
        );
    }
}
