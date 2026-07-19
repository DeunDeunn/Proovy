package com.deundeun.chat.domain;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMessage {

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private ChatMessageType messageType;
    private ChatReferenceType referenceType;
    private Long referenceId;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;

    private ChatMessage(Long chatRoomId, Long senderId, String content,
                        ChatMessageType messageType, ChatReferenceType referenceType, Long referenceId) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = LocalDateTime.now();
    }

    public static ChatMessage create(Long chatRoomId, Long senderId, String content,
                                     ChatMessageType messageType, ChatReferenceType referenceType, Long referenceId) {
        return new ChatMessage(chatRoomId, senderId, content, messageType, referenceType, referenceId);
    }

    public void delete() {
        if (isDeleted()) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_ALREADY_DELETED);
        }

        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
