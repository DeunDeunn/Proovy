package com.deundeun.notification.event;

public record SettlementCompletedEvent(
    Long userId,
    Long settlementId
) {
}
