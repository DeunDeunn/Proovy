package com.deundeun.chat.constant;

public final class ChatStompDestinations {

    private static final String ROOM_TOPIC_PREFIX = "/topic/chats/rooms/";

    public static final String ROOM_TOPIC_PATTERN = ROOM_TOPIC_PREFIX + "{chatRoomId}";
    public static final String PERSONAL_ERROR_QUEUE = "/queue/errors";
    public static final String PERSONAL_ROOM_UPDATE_QUEUE = "/queue/chat-room-updates";

    public static String roomTopic(Long chatRoomId) {
        return ROOM_TOPIC_PREFIX + chatRoomId;
    }

    private ChatStompDestinations() {
    }
}
