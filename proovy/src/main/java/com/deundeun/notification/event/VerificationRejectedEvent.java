package com.deundeun.notification.event;

import com.deundeun.notification.domain.NotificationType;

public record VerificationRejectedEvent(
    Long userId,
    Long verificationPostId
) implements NotificationEvent {

    @Override
    public NotificationType type() {
        return NotificationType.VERIFICATION_REJECTED;
    }

    public String eventKey() {
        return eventKey(verificationPostId);
    }
}
