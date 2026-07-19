package com.deundeun.certification.enums;

// 피드 정렬 (챌린지/전체 피드만 사용. 내/타인 피드는 최신순 고정)
public enum FeedSort {
    LATEST,   // 최신순 id DESC
    POPULAR;  // 인기순 like_count DESC, id DESC

    // 기본 최신
    public static FeedSort from(String value) {
        if (value == null) {
            return LATEST;
        }
        try {
            return FeedSort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LATEST;
        }
    }
}
