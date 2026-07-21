package com.deundeun.chat.dto.response;

public record ChatRoomUpdatedEvent(
    String eventType,
    Long chatRoomId
) {
    private static final String EVENT_TYPE = "ROOM_UPDATED";

    public static ChatRoomUpdatedEvent of(Long chatRoomId) {
        return new ChatRoomUpdatedEvent(EVENT_TYPE, chatRoomId);
    }
}
