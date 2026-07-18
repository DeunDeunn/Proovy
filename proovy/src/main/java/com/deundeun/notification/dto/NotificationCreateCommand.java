package com.deundeun.notification.dto;

import com.deundeun.notification.domain.NotificationType;
import com.deundeun.notification.domain.TargetType;

public record NotificationCreateCommand(
    Long userId,
    NotificationType type,
    String title,
    String content,
    TargetType targetType,
    Long targetId,
    String eventKey
) {
}
