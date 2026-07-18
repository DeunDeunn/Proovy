package com.deundeun.chat.dto.response;

import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.dto.ChatRoomListItem;

import java.time.LocalDateTime;

public record LastMessageResponse(
    Long messageId,
    Long senderId,
    String senderNickname,
    String content,
    ChatMessageType messageType,
    LocalDateTime deletedAt,
    LocalDateTime createdAt
) {

    public static LastMessageResponse of(ChatRoomListItem item, String senderNickname) {
        if (item.lastMessageId() == null) {
            return null;
        }

        boolean deleted = item.lastMessageDeletedAt() != null;

        return new LastMessageResponse(
            item.lastMessageId(),
            item.lastMessageSenderId(),
            senderNickname,
            deleted ? null : item.lastMessageContent(),
            item.lastMessageType(),
            item.lastMessageDeletedAt(),
            item.lastMessageCreatedAt()
        );
    }

    public static LastMessageResponse of(ChatMessage message, String senderNickname) {
        if (message == null) {
            return null;
        }

        boolean deleted = message.getDeletedAt() != null;

        return new LastMessageResponse(
            message.getId(),
            message.getSenderId(),
            senderNickname,
            deleted ? null : message.getContent(),
            message.getMessageType(),
            message.getDeletedAt(),
            message.getCreatedAt()
        );
    }
}
