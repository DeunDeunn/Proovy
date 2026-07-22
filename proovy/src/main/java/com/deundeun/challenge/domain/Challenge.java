package com.deundeun.challenge.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {

    private static final int DEFAULT_SUCCESS_CRITERIA_RATE = 80;

    private Long id;
    private Long hostId; // users.id (도메인 밖, FK 없음)
    private String title;
    private String description;
    private Long categoryId;
    private Long entryFee;
    private String verificationMethod;
    @Builder.Default
    private CertFrequency certFrequency = CertFrequency.DAILY;
    private Integer dailyCertLimit;
    @Builder.Default
    private Integer successCriteriaRate = DEFAULT_SUCCESS_CRITERIA_RATE;
    private boolean aiReviewEnabled;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxParticipants;
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.RECRUITING;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalTime certStartTime;
    private LocalTime certEndTime;
    private FeedVisibility feedVisibility;
    private String thumbnailUrl;

}
