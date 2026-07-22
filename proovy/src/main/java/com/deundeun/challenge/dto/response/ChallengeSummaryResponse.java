package com.deundeun.challenge.dto.response;

import com.deundeun.challenge.domain.ChallengeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeSummaryResponse {

    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private Long entryFee;
    private Integer currentParticipants;
    private Integer maxParticipants;
    private LocalDate startDate;
    private LocalDate endDate;
    private ChallengeStatus status;
    private String thumbnailUrl;

}
