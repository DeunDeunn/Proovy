package com.deundeun.chat.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatAttachment {

    private Long id;
    private Long messageId;
    private Long uploaderId;
    private String fileUrl;
    private String originalFileName;
    private ChatFileType fileType;
    private Long fileSize;
    private LocalDateTime createdAt;

    private ChatAttachment(Long messageId, Long uploaderId, String fileUrl,
                           String originalFileName, ChatFileType fileType, Long fileSize) {
        this.messageId = messageId;
        this.uploaderId = uploaderId;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.createdAt = LocalDateTime.now();
    }

    public static ChatAttachment create(Long messageId, Long uploaderId, String fileUrl,
                                        String originalFileName, ChatFileType fileType, Long fileSize) {
        return new ChatAttachment(messageId, uploaderId, fileUrl, originalFileName, fileType, fileSize);
    }
}
