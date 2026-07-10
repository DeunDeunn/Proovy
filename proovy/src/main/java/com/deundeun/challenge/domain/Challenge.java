package com.deundeun.challenge.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Challenge {

    private Long id;
    private Long hostId; // users.id (도메인 밖, FK 없음)
    private String title;
    private String description;
    private Long categoryId;
    private Long entryFee;
    private String verificationMethod;
    private CertFrequency certFrequency;
    private Integer dailyCertLimit;
    private Integer successCriteriaRate;
    private Boolean aiReviewEnabled;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxParticipants;
    private ChallengeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
