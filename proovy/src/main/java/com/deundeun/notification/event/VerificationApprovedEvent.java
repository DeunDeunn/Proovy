package com.deundeun.notification.event;

public record VerificationApprovedEvent(
    Long userId,
    Long verificationPostId
) {
}
