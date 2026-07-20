package com.deundeun.auth.dto.response;

import com.deundeun.auth.domain.UserVerificationStatus;

import java.time.LocalDateTime;

public record UserVerificationStatusResponse(
    UserVerificationStatus status,
    LocalDateTime appliedAt,
    LocalDateTime approvedAt,
    String rejectionReason,
    long successCount,
    long requiredCount
) {
}
