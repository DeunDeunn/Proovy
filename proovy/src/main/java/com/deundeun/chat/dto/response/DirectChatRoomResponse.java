package com.deundeun.chat.dto.response;

import java.time.LocalDateTime;

import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.domain.ChatRoomType;

public record DirectChatRoomResponse(
    Long chatRoomId,
    ChatRoomType chatRoomType,
    String directChatKey,
    boolean created,
    LocalDateTime createdAt
) {

    public static DirectChatRoomResponse of(ChatRoom room, boolean created) {
        return new DirectChatRoomResponse(
            room.getId(),
            room.getType(),
            room.getDirectChatKey(),
            created,
            room.getCreatedAt()
        );
    }
}
