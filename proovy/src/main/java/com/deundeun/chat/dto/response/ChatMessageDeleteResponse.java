package com.deundeun.chat.dto.response;

import com.deundeun.chat.domain.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageDeleteResponse(
    Long messageId,
    Long chatRoomId,
    LocalDateTime deletedAt
) {
    public static ChatMessageDeleteResponse of(ChatMessage message) {
        return new ChatMessageDeleteResponse(message.getId(), message.getChatRoomId(), message.getDeletedAt());
    }
}
