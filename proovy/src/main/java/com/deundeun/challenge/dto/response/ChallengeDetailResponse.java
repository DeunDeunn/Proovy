package com.deundeun.challenge.dto.response;

import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.domain.FeedVisibility;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeDetailResponse {

    private Long id;
    private Long hostId;
    private String hostNickname;         // users JOIN
    private String title;
    private String description;
    private String categoryName;         // categories JOIN
    private Long entryFee;
    private String verificationMethod;
    private Integer dailyCertLimit;
    private Integer successCriteriaRate;
    private Boolean aiReviewEnabled;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer currentParticipants; // COUNT 서브쿼리
    private Integer maxParticipants;
    private ChallengeStatus status;
    private LocalTime certStartTime;
    private LocalTime certEndTime;
    private FeedVisibility feedVisibility;
    private LocalDateTime createdAt;

}
