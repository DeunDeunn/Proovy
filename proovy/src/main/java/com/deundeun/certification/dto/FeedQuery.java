package com.deundeun.certification.dto;

import com.deundeun.certification.enums.FeedFilter;
import com.deundeun.certification.enums.FeedSort;
import lombok.Data;

// 파라미터 여러개 안쓸라고 한거 피드 조회용
@Data
public class FeedQuery {
    private Long cursor;         // 마지막으로 받은 아이디 페이징용
    private Long cursorLike;     // 인기순 커서 복합키: 마지막 항목의 좋아요집계
    private int size;
    private Long viewerId;       // 타인피드 접근 판단
    private Long challengeId;    // 챌린지 피드에서만
    private Long targetUserId;   // 타인 피드 유저
    private FeedFilter filter;
    private FeedSort sort;       // 정렬 -최신,인기/ 정렬은 무조건 기본 최신

    // 피드별 분기
    private boolean includeAllStatus;      //내 피드만
    private boolean publicOnly;            //전체 피드만
    private boolean applyViewerVisibility; //타인 피드만
}
