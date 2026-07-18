package com.deundeun.notification.event;

import com.deundeun.notification.domain.NotificationType;

public record VerificationApprovedEvent(
    Long userId,
    Long verificationPostId
) implements NotificationEvent {

    @Override
    public NotificationType type() {
        return NotificationType.VERIFICATION_APPROVED;
    }

    public String eventKey() {
        return eventKey(verificationPostId);
    }
}
