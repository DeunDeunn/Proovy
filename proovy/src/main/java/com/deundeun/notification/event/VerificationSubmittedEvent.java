package com.deundeun.notification.event;

import com.deundeun.notification.domain.NotificationType;

public record VerificationSubmittedEvent(
    Long userId,
    Long verificationPostId
) implements NotificationEvent {

    @Override
    public NotificationType type() {
        return NotificationType.VERIFICATION_SUBMITTED;
    }

    public String eventKey() {
        return eventKey(verificationPostId);
    }
}
