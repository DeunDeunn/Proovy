package com.deundeun.chat.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoom {

    private Long id;
    private Long challengeId;
    private String directChatKey;
    private ChatRoomType type;
    private LocalDateTime createdAt;

    private ChatRoom(Long challengeId, String directChatKey, ChatRoomType type) {
        this.challengeId = challengeId;
        this.directChatKey = directChatKey;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public static ChatRoom createChallengeRoom(Long challengeId) {
        return new ChatRoom(challengeId, null, ChatRoomType.CHALLENGE);
    }

    public static ChatRoom createDirectRoom(Long userId1, Long userId2) {
        return new ChatRoom(null, buildDirectChatKey(userId1, userId2), ChatRoomType.DIRECT);
    }

    private static String buildDirectChatKey(Long userId1, Long userId2) {
        long min = Math.min(userId1, userId2);
        long max = Math.max(userId1, userId2);
        return min + ":" + max;
    }
}
