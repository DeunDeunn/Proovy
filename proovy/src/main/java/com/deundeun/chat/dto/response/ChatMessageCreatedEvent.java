package com.deundeun.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record ChatMessageCreatedEvent(
    String eventType,
    @JsonUnwrapped ChatMessageResponse message
) {
    private static final String EVENT_TYPE = "MESSAGE_CREATED";

    public static ChatMessageCreatedEvent of(ChatMessageResponse message) {
        return new ChatMessageCreatedEvent(EVENT_TYPE, message);
    }
}
