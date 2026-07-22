package com.deundeun.certification.dto;

/**
 * 홈 화면의 오늘 인증 현황.
 * certifiedChallengeCount / inProgressChallengeCount 형태로 표시한다.
 */
public record TodayCertificationProgressResponse(
        int certifiedChallengeCount,
        int inProgressChallengeCount
) {
}
