package com.deundeun.certification.dto;

import lombok.Data;


//참가자 조회
//이 유저가 챌린지의 정상 참가자인지 확인하고, 인증글 저장용 참가자 아이디얻기

@Data
public class ParticipantForCertification {
    private Long id;        // 참가자아이디
    private String status;  // 'ACTIVE' / 'WITHDRAWN' / 'KICKED'
}
