package com.deundeun.challenge.dto.request;

import com.deundeun.challenge.domain.ChallengeSort;
import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.domain.SortDirection;

public record ChallengeSearchCondition(
        Long categoryId,
        ChallengeStatus status,
        String keyword,
        ChallengeSort sort,
        SortDirection direction,
        Integer page,
        Integer size
) {

    public ChallengeSearchCondition {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1 || size > 50) size = 10;
        if (sort == null) sort = ChallengeSort.LATEST;
        if (direction == null) direction = defaultDirection(sort);
    }

    // 정렬 기준별 기본 방향: 최신순은 최근이 먼저, 참가비는 저렴한 게 먼저, 참가자는 많은 게 먼저
    private static SortDirection defaultDirection(ChallengeSort sort) {
        return sort == ChallengeSort.ENTRY_FEE ? SortDirection.ASC : SortDirection.DESC;
    }

    public long offset() {
            return (long) page * size;
    }

}
