package com.deundeun.notification.event;

import com.deundeun.notification.domain.NotificationType;

public record SettlementCompletedEvent(
    Long userId,
    Long settlementId
) implements NotificationEvent {

    @Override
    public NotificationType type() {
        return NotificationType.SETTLEMENT_COMPLETED;
    }

    public String eventKey() {
        return eventKey(settlementId);
    }
}
