package com.deundeun.challenge.dto.request;

import com.deundeun.challenge.domain.FeedVisibility;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

// PATCH 부분 수정 요청 — null인 필드는 "변경하지 않음"을 의미한다
public record ChallengeUpdateRequest(

        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        String description,

        Long categoryId,

        @Positive(message = "참가비는 0보다 커야 합니다.")
        Long entryFee,

        String verificationMethod,

        @Positive(message = "일일 인증 횟수는 0보다 커야 합니다.")
        Integer dailyCertLimit,

        LocalDate startDate,

        LocalDate endDate,

        @Positive(message = "모집 정원은 0보다 커야 합니다.")
        Integer maxParticipants,

        LocalTime certStartTime,

        LocalTime certEndTime,

        FeedVisibility feedVisibility
) {

    // 수정할 필드가 하나라도 있는지 (전부 null인 빈 요청 거부용)
    public boolean hasAnyChanges() {
        return title != null || description != null || hasCoreChanges();
    }

    // 참가자가 있으면 수정할 수 없는 핵심 조건이 하나라도 포함됐는지 (제목/설명 외 전부)
    public boolean hasCoreChanges() {
        return categoryId != null || entryFee != null || verificationMethod != null
                || dailyCertLimit != null || startDate != null || endDate != null
                || maxParticipants != null || certStartTime != null || certEndTime != null
                || feedVisibility != null;
    }
}
