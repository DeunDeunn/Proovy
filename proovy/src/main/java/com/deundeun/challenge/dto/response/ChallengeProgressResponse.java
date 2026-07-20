package com.deundeun.challenge.dto.response;

import com.deundeun.challenge.domain.ChallengeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeProgressResponse {

    private Long id;
    private String title;
    private ChallengeStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer currentParticipants;
    private Integer maxParticipants;
    private Integer successCount;
    private Integer failCount;

    private Integer totalDays;
    private Integer daysElapsed;
    private Integer progressPercentage;

    public void calculateProgress() {
        LocalDate today = LocalDate.now();
        this.totalDays = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;

        LocalDate clampedToday = today.isBefore(startDate) ? startDate : (today.isAfter(endDate) ? endDate : today);
        this.daysElapsed = (int) (clampedToday.toEpochDay() - startDate.toEpochDay()) + 1;
        if (today.isBefore(startDate)) {
            this.daysElapsed = 0;
        }

        this.progressPercentage = totalDays == 0 ? 0 : Math.min(100, (daysElapsed * 100) / totalDays);
    }
}
