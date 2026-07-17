package com.deundeun.chat.dto.response;

import com.deundeun.chat.domain.*;

import java.time.LocalDateTime;

public record ChallengeChatRoomResponse(
    Long chatRoomId,
    ChatRoomType chatRoomType,
    Long challengeId,
    String challengeTitle,
    int memberCount,
    LastMessageResponse lastMessage,
    int unreadCount,
    Long lastReadMessageId,
    LocalDateTime lastReadAt,
    LocalDateTime createdAt
) {

    public static ChallengeChatRoomResponse of(ChatRoom room, String challengeTitle, int memberCount,
                                                LastMessageResponse lastMessage, int unreadCount, ChatRoomMember member) {
        return new ChallengeChatRoomResponse(
            room.getId(),
            room.getType(),
            room.getChallengeId(),
            challengeTitle,
            memberCount,
            lastMessage,
            unreadCount,
            member.getLastReadMessageId(),
            member.getLastReadAt(),
            room.getCreatedAt()
        );
    }
}
