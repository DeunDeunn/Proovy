package com.deundeun.chat.dto.request;

import com.deundeun.chat.domain.ChatMessageType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ChatMessageSendRequest(
    @NotNull(message = "메시지 타입을 입력해주세요.")
    ChatMessageType messageType,

    String content,
    List<Long> attachmentIds
) {
}
