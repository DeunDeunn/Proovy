package com.deundeun.chat.dto.response;

import com.deundeun.chat.domain.ChatRoomMember;

import java.time.LocalDateTime;

public record ChatRoomReadResponse(
    Long chatRoomId,
    Long userId,
    Long lastReadMessageId,
    LocalDateTime lastReadAt
) {

    public static ChatRoomReadResponse of(ChatRoomMember member) {
        return new ChatRoomReadResponse(
            member.getChatRoomId(),
            member.getUserId(),
            member.getLastReadMessageId(),
            member.getLastReadAt()
        );
    }
}
