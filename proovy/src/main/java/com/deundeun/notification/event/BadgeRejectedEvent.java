package com.deundeun.notification.event;

public record BadgeRejectedEvent(
    Long userId,
    Long badgeApplicationId
) {
}
