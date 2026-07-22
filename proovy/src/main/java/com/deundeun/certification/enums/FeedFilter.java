package com.deundeun.certification.enums;

// 챌린지/전체 피드의 필터 (내 피드,타인 피드에는 없음)
public enum FeedFilter {
    ALL,     // 전체
    MINE,    // 내 글만
    TODAY,   // 오늘 글만
    REVIEW;  // 방장·관리자 검수용 (PENDING, 오래된 순)

    // 기본 전체
    public static FeedFilter from(String value) {
        if (value == null) {
            return ALL;
        }
        try {
            return FeedFilter.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
