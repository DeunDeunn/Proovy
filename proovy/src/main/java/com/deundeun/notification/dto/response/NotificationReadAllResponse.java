package com.deundeun.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationReadAllResponse(
    int updatedCount,
    LocalDateTime readAt
) {
}
