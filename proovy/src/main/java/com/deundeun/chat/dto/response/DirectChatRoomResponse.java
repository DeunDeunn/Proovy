package com.deundeun.chat.dto.response;

import java.time.LocalDateTime;

import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.domain.ChatRoomType;

public record DirectChatRoomResponse(
    Long chatRoomId,
    ChatRoomType chatRoomType,
    String directChatKey,
    boolean created,
    DirectChatPartnerResponse partner,
    LastMessageResponse lastMessage,
    int unreadCount,
    Long lastReadMessageId,
    LocalDateTime lastReadAt,
    LocalDateTime createdAt
) {

    public static DirectChatRoomResponse of(ChatRoom room, boolean created, DirectChatPartnerResponse partner,
                                             LastMessageResponse lastMessage, int unreadCount,
                                             Long lastReadMessageId, LocalDateTime lastReadAt) {
        return new DirectChatRoomResponse(
            room.getId(),
            room.getType(),
            room.getDirectChatKey(),
            created,
            partner,
            lastMessage,
            unreadCount,
            lastReadMessageId,
            lastReadAt,
            room.getCreatedAt()
        );
    }
}
