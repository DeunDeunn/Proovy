package com.deundeun.auth.dto;

import com.deundeun.auth.domain.UserVerificationStatus;

public record UserVerificationReviewRequest(UserVerificationStatus status, String rejectionReason) {
}
