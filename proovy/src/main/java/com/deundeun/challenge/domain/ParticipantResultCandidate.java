package com.deundeun.challenge.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 챌린지 종료 처리 시점에 참가자별 성공/실패를 판정하기 위한 중간 집계 데이터.
 * approvedCount = 승인(APPROVED)된 인증글 수.
 */
@Getter
@Setter
@NoArgsConstructor
public class ParticipantResultCandidate {

    private Long id; // challenge_participants.id
    private Long userId;
    private Integer approvedCount;
}
