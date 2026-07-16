package com.deundeun.chat.dto.request;

import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatReferenceType;
import jakarta.validation.constraints.NotNull;

public record ChatMessageSendRequest(
    @NotNull(message = "메시지 타입을 입력해주세요.")
    ChatMessageType messageType,

    String content,
    ChatReferenceType referenceType,
    Long referenceId
) {
}
