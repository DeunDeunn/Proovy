package com.deundeun.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationDeleteAllResponse(
    int deletedCount,
    LocalDateTime deletedAt
) {
}
