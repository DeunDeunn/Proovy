package com.deundeun.chat.dto.response;

import com.deundeun.auth.domain.User;
import com.deundeun.chat.domain.ChatAttachment;
import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatReferenceType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(
    Long messageId,
    Long chatRoomId,
    Long senderId,
    String senderNickname,
    String senderProfileImage,
    boolean senderBadgeApproved,
    String content,
    ChatMessageType messageType,
    ChatReferenceType referenceType,
    Long referenceId,
    SharedCertificationResponse sharedCertification,
    List<ChatAttachmentResponse> attachments,
    boolean read,
    LocalDateTime deletedAt,
    LocalDateTime createdAt
) {

    public static ChatMessageResponse of(ChatMessage message, User sender, List<ChatAttachment> attachments,
                                          SharedCertificationResponse sharedCertification, boolean senderBadgeApproved,
                                          boolean read) {
        boolean deleted = message.getDeletedAt() != null;

        return new ChatMessageResponse(
            message.getId(),
            message.getChatRoomId(),
            message.getSenderId(),
            sender != null ? sender.getNickname() : null,
            sender != null ? sender.getProfileImageUrl() : null,
            senderBadgeApproved,
            deleted ? null : message.getContent(),
            message.getMessageType(),
            deleted ? null : message.getReferenceType(),
            deleted ? null : message.getReferenceId(),
            deleted ? null : sharedCertification,
            deleted ? List.of() : attachments.stream().map(ChatAttachmentResponse::of).toList(),
            read,
            message.getDeletedAt(),
            message.getCreatedAt()
        );
    }
}
