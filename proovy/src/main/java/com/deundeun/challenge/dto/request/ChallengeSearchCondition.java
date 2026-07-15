package com.deundeun.challenge.dto.request;

import com.deundeun.challenge.domain.ChallengeStatus;

public record ChallengeSearchCondition(
        Long categoryId,
        ChallengeStatus status,
        String keyword,
        Integer page,
        Integer size
) {

    public ChallengeSearchCondition {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1 || size > 50)size = 10;
    }

    public long offset() {
            return (long) page * size;
    }

}
