package com.deundeun.certification.dto;

/**
 * 현재 진행 중인 챌린지에서 이어지고 있는 승인 인증 연속일 수.
 */
public record CertificationStreakResponse(int currentStreakDays) {
}
