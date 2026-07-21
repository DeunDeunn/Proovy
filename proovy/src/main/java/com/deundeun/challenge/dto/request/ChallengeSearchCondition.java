package com.deundeun.challenge.dto.request;

import com.deundeun.challenge.domain.ChallengeSort;
import com.deundeun.challenge.domain.ChallengeStatus;

public record ChallengeSearchCondition(
        Long categoryId,
        ChallengeStatus status,
        String keyword,
        ChallengeSort sort,
        Integer page,
        Integer size
) {

    public ChallengeSearchCondition {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1 || size > 50)size = 10;
        if (sort == null) sort = ChallengeSort.LATEST;
    }

    public long offset() {
            return (long) page * size;
    }

}
