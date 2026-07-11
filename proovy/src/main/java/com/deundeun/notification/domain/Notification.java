package com.deundeun.notification.domain;

import com.deundeun.notification.dto.NotificationCreateCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    private Notification(NotificationCreateCommand command) {
        this.userId = command.userId();
        this.type = command.type();
        this.title = command.title();
        this.content = command.content();
        this.targetType = command.targetType();
        this.targetId = command.targetId();
        this.eventKey = command.eventKey();
    }

    public static Notification create(NotificationCreateCommand command) {
        return new Notification(command);
    }
}
