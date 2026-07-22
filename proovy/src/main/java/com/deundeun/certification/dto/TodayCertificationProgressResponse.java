package com.deundeun.certification.dto;

/**
 * 홈 화면의 오늘 인증 현황.
 * 참여 중인 챌린지의 인증 진행도와, 내가 운영 중인 챌린지의 인증글 현황을 함께 표시한다.
 */
public record TodayCertificationProgressResponse(
        int certifiedChallengeCount,
        int inProgressChallengeCount,
        long hostedTodayCertificationPostCount,
        long hostedPendingCertificationPostCount,
        // 미검수 인증이 있는 IN_PROGRESS 운영 챌린지가 딱 하나면 그 챌린지 ID, 아니면 null.
        // 홈의 미검수 CTA가 단일 챌린지 인증 관리로 직행할지(값 존재), 목록으로 보낼지(null) 판단에 사용.
        Long hostedPendingCertificationChallengeId
) {
}
