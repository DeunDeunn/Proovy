package com.deundeun.notification.event;

public record VerificationSubmittedEvent(
    Long userId,
    Long verificationPostId
) {
}
