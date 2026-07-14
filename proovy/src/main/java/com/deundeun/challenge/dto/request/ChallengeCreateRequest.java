package com.deundeun.challenge.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ChallengeCreateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        String description,

        @NotNull(message = "카테고리는 필수입니다.")
        Long categoryId,

        @NotNull(message = "참가비는 필수입니다.")
        @Positive(message = "참가비는 0보다 커야 합니다.")
        Long entryFee,

        @NotBlank(message = "인증 방법은 필수입니다.")
        String verificationMethod,

        @NotNull(message = "일일 인증 횟수는 필수입니다.")
        @Positive(message = "일일 인증 횟수는 0보다 커야 합니다.")
        Integer dailyCertLimit,

        @NotNull(message = "AI 검수 사용 여부는 필수입니다.")
        Boolean aiReviewEnabled,

        @NotNull(message = "시작일은 필수입니다.")
        @FutureOrPresent(message = "시작일은 오늘 이후여야 합니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        @NotNull(message = "모집 정원은 필수입니다.")
        @Positive(message = "모집 정원은 0보다 커야 합니다.")
        Integer maxParticipants
) {
}