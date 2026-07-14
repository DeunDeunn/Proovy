package com.deundeun.notification.event;

import com.deundeun.notification.domain.NotificationType;

public interface NotificationEvent {

    NotificationType type();

    Long userId();

    String eventKey();

    default String eventKey(Object id) {
        return "%s:%s:USER:%s".formatted(type().name(), id, userId());
    }
}
