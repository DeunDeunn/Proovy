package com.deundeun.certification.dto;

/**
 * 홈 화면의 오늘 인증 현황.
 * 참여 중인 챌린지의 인증 진행도와, 내가 운영 중인 챌린지의 인증글 현황을 함께 표시한다.
 */
public record TodayCertificationProgressResponse(
        int certifiedChallengeCount,
        int inProgressChallengeCount,
        long hostedTodayCertificationPostCount,
        long hostedPendingCertificationPostCount
) {
}
