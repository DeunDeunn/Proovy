package com.deundeun.certification.dto;

import lombok.Data;

import java.time.LocalTime;

//챌린지 조회
//등록 전에 챌린지 존재 여부와 상태를 확인해야함
@Data
public class ChallengeForCertification {
    private Long id;        // 챌린지아이디
    private String status;  // 챌린지방 상태
    private Long hostId;    // 방장
    private LocalTime certStartTime;  // 인증 등록 가능 시작
    private LocalTime certEndTime;    // 인증 등록 가능 종료
    private String feedVisibility;    // 피드 공개 설정
}
