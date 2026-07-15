package com.deundeun.chat.dto;

import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatRoomType;

import java.time.LocalDateTime;

// "내 채팅방 목록" 조회 쿼리 결과 담음
public record ChatRoomListItem(
    Long chatRoomId,
    ChatRoomType chatRoomType,
    Long challengeId,
    String directChatKey,
    LocalDateTime roomCreatedAt,
    Long lastReadMessageId,
    LocalDateTime lastReadAt,
    Long lastMessageId,
    Long lastMessageSenderId,
    String lastMessageContent,
    ChatMessageType lastMessageType,
    LocalDateTime lastMessageDeletedAt,
    LocalDateTime lastMessageCreatedAt
) {
}
