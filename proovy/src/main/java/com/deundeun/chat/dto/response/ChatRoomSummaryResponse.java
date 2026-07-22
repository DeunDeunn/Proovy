package com.deundeun.chat.dto.response;

import com.deundeun.chat.domain.ChatRoomType;
import com.deundeun.chat.dto.ChatRoomListItem;

import java.time.LocalDateTime;

public record ChatRoomSummaryResponse(
    Long chatRoomId,
    ChatRoomType chatRoomType,
    Long challengeId,
    String challengeTitle,
    String challengeThumbnailUrl,
    DirectChatPartnerResponse directChatPartner,
    LastMessageResponse lastMessage,
    int unreadCount,
    Long lastReadMessageId,
    LocalDateTime lastReadAt,
    LocalDateTime createdAt
) {

    public static ChatRoomSummaryResponse of(ChatRoomListItem item, String challengeTitle, String challengeThumbnailUrl,
                                              DirectChatPartnerResponse directChatPartner,
                                              String lastMessageSenderNickname, int unreadCount) {
        return new ChatRoomSummaryResponse(
            item.chatRoomId(),
            item.chatRoomType(),
            item.challengeId(),
            challengeTitle,
            challengeThumbnailUrl,
            directChatPartner,
            LastMessageResponse.of(item, lastMessageSenderNickname),
            unreadCount,
            item.lastReadMessageId(),
            item.lastReadAt(),
            item.roomCreatedAt()
        );
    }
}
