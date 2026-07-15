package com.deundeun.notification.event;

import com.deundeun.notification.dto.response.NotificationResponse;

public record NotificationCreatedEvent(
    Long userId,
    NotificationResponse notification
) {
}
