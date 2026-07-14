package com.deundeun.certification.dto;

import lombok.Data;

//챌린지 조회
//등록 전에 챌린지 존재 여부와 상태를 확인해야함
@Data
public class ChallengeForCertification {
    private Long id;        // 챌린지 id
    private String status;  // 'RECRUITING' / 'IN_PROGRESS' / 'COMPLETED' / 'CANCELLED'
    private Long hostId;    // 방장 user_id (등록 시 방장에게 알림 보낼 대상)
}
