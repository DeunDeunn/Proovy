package com.deundeun.notification.event;

public record BadgeApprovedEvent(
    Long userId,
    Long badgeApplicationId
) {
}
