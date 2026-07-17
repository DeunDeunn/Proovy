package com.deundeun.chat.event;

import com.deundeun.global.exception.ErrorCode;

public record ChatSubscriptionRejectedEvent(
    String username,
    ErrorCode errorCode
) {
}
