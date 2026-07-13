package com.deundeun.chat.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomMember {
    private Long id;
    private Long chatRoomId;
    private Long userId;
    private Long lastReadMessageId;
    private LocalDateTime lastReadAt;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    private ChatRoomMember(Long chatRoomId, Long userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.joinedAt = LocalDateTime.now();
    }

    public static ChatRoomMember join(Long chatRoomId, Long userId) {
        return new ChatRoomMember(chatRoomId, userId);
    }
}
