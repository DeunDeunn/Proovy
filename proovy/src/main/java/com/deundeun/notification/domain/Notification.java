package com.deundeun.notification.domain;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class Notification {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String content;
    private TargetType targetType;
    private Long targetId;
    private String eventKey;
    private LocalDateTime readAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;

    private Notification(Long userId, NotificationType type, String title, String content,
                         TargetType targetType, Long targetId, String eventKey) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.title = requireNonBlank(title, "title");
        this.content = requireNonBlank(content, "content");
        this.eventKey = requireNonBlank(eventKey, "eventKey");
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdAt = LocalDateTime.now();
    }

    public static Notification create(Long userId, NotificationType type, String title, String content,
                                      TargetType targetType, Long targetId, String eventKey) {
        return new Notification(userId, type, title, content, targetType, targetId, eventKey);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
