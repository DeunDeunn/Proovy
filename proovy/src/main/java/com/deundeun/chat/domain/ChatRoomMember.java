package com.deundeun.chat.domain;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
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

    public void leave() {
        if (!isActive()) {
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_LEFT);
        }

        this.leftAt = LocalDateTime.now();
    }

    public void rejoin() {
        if (isActive()) {
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        }

        this.leftAt = null;
        this.joinedAt = LocalDateTime.now();
    }

    public void markRead(Long lastReadMessageId) {
        if (!canAdvanceReadCursor(lastReadMessageId)) {
            throw new ApiException(ErrorCode.CHAT_INVALID_READ_CURSOR);
        }

        this.lastReadMessageId = lastReadMessageId;
        this.lastReadAt = LocalDateTime.now();
    }

    public boolean canAdvanceReadCursor(Long lastReadMessageId) {
        return lastReadMessageId != null
            && (this.lastReadMessageId == null || this.lastReadMessageId < lastReadMessageId);
    }

    public boolean isActive() {
        return this.leftAt == null;
    }
}
