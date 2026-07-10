package com.deundeun.notification.event;

public record VerificationRejectedEvent(
    Long userId,
    Long verificationPostId
) {
}
