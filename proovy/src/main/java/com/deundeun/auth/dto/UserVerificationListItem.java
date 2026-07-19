package com.deundeun.auth.dto;

import com.deundeun.auth.domain.UserVerificationStatus;

import java.time.LocalDateTime;

public record UserVerificationListItem(
    Long id,
    Long userId,
    String nickname,
    UserVerificationStatus status,
    LocalDateTime appliedAt
) {
}
