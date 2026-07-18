package com.deundeun.notification.event;

import com.deundeun.notification.domain.NotificationType;

public record BadgeRejectedEvent(
    Long userId,
    Long badgeApplicationId
) implements NotificationEvent {

    @Override
    public NotificationType type() {
        return NotificationType.BADGE_REJECTED;
    }

    public String eventKey() {
        return eventKey(badgeApplicationId);
    }
}
