package com.deundeun.chat.dto.response;

import com.deundeun.chat.domain.ChatAttachment;
import com.deundeun.chat.domain.ChatFileType;

public record ChatAttachmentResponse(
    Long attachmentId,
    String fileUrl,
    String originalFileName,
    ChatFileType fileType,
    Long fileSize
) {

    public static ChatAttachmentResponse of(ChatAttachment attachment) {
        return new ChatAttachmentResponse(
            attachment.getId(),
            attachment.getFileUrl(),
            attachment.getOriginalFileName(),
            attachment.getFileType(),
            attachment.getFileSize()
        );
    }
}
