package com.deundeun.notification.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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

    public static Notification create(Long userId, NotificationType type, String title, String content,
                                       TargetType targetType, Long targetId, String eventKey) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setEventKey(eventKey);
        return notification;
    }
}
