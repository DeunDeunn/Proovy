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
    private LocalTime certStartTime;
    private LocalTime certEndTime;
    private FeedVisibility feedVisibility;
    private String thumbnailUrl;

}
