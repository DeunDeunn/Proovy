package com.deundeun.notification.domain;

import java.util.List;

public enum NotificationCategory {
    VERIFICATION(List.of(
        NotificationType.VERIFICATION_SUBMITTED,
        NotificationType.VERIFICATION_APPROVED,
        NotificationType.VERIFICATION_REJECTED
    )),
    SETTLEMENT(List.of(
        NotificationType.SETTLEMENT_COMPLETED,
        NotificationType.HOST_REVENUE_PAID
    )),
    ETC(List.of(
        NotificationType.BADGE_APPROVED,
        NotificationType.BADGE_REJECTED
    ));

    private final List<NotificationType> types;

    NotificationCategory(List<NotificationType> types) {
        this.types = types;
    }

    public List<NotificationType> getTypes() {
        return types;
    }
}
