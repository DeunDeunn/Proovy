package com.deundeun.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVerification {
    private Long id;
    private Long userId;
    private UserVerificationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime approvedAt;
    private String rejectionReason;
}
