package com.deundeun.notification.event;

import com.deundeun.notification.domain.NotificationType;

public record HostRevenuePaidEvent(
    Long userId,
    Long hostRevenueId
) implements NotificationEvent {

    @Override
    public NotificationType type() {
        return NotificationType.HOST_REVENUE_PAID;
    }

    public String eventKey() {
        return eventKey(hostRevenueId);
    }
}
