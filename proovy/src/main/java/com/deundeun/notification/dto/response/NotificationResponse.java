package com.deundeun.notification.dto.response;

import com.deundeun.notification.domain.Notification;
import com.deundeun.notification.domain.NotificationType;
import com.deundeun.notification.domain.TargetType;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String content,
    TargetType targetType,
    Long targetId,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getContent(),
            notification.getTargetType(),
            notification.getTargetId(),
            notification.getReadAt(),
            notification.getCreatedAt()
        );
    }
}
