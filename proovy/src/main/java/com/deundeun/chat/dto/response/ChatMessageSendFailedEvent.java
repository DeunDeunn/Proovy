package com.deundeun.chat.dto.response;

import com.deundeun.global.exception.ErrorCode;

public record ChatMessageSendFailedEvent(
    String eventType,
    int status,
    String code,
    String message
) {
    private static final String EVENT_TYPE = "CHAT_MESSAGE_SEND_FAILED";

    public static ChatMessageSendFailedEvent of(ErrorCode errorCode) {
        return new ChatMessageSendFailedEvent(EVENT_TYPE, errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage());
    }
}
