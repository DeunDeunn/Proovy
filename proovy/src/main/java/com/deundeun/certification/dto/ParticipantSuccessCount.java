package com.deundeun.certification.dto;

import lombok.Data;

// [조회 결과] 참가자별 APPROVED 인증 일수.
// 챌린지 도메인이 성공/실패 판정에 쓰는 연동용 — 판정 자체는 챌린지 도메인 관할.
@Data
public class ParticipantSuccessCount {
    private Long participantId;   // challenge_participants.id
    private int successCount;     // APPROVED 인증한 날 수
}
