package com.deundeun.chat.dto.response;

import com.deundeun.global.exception.ErrorCode;

public record ChatSubscribeFailedEvent(
    String eventType,
    int status,
    String code,
    String message
) {
    private static final String EVENT_TYPE = "CHAT_SUBSCRIBE_FAILED";

    public static ChatSubscribeFailedEvent of(ErrorCode errorCode) {
        return new ChatSubscribeFailedEvent(EVENT_TYPE, errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage());
    }
}
