package com.deundeun.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationReadResponse(
    Long id,
    LocalDateTime readAt
) {
}
