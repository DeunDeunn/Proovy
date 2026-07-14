package com.deundeun.certification.dto;

import lombok.Data;

import java.time.LocalTime;

//챌린지 조회
//등록 전에 챌린지 존재 여부와 상태를 확인해야함
@Data
public class ChallengeForCertification {
    private Long id;        // 챌린지 id
    private String status;  // 'RECRUITING' / 'IN_PROGRESS' / 'COMPLETED' / 'CANCELLED'
    private Long hostId;    // 방장
    private LocalTime certStartTime;  // 인증 등록 가능 시작 시각 (challenges.cert_start_time)
    private LocalTime certEndTime;    // 인증 등록 가능 종료 시각 (challenges.cert_end_time)
}
