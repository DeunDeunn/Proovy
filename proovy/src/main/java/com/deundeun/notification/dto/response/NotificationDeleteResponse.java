package com.deundeun.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationDeleteResponse(
    Long id,
    LocalDateTime deletedAt
) {
}
