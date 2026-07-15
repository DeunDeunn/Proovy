package com.deundeun.certification.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Data
public class CreateCertificationPostSqlParam {

    private Long id;                     //인증글 아이디
    private Long userId;
    private Long challengeParticipantId; // 참가자아이디
    private String contents;             // 본문 글
    private String thumbnailImage;       // 대표 이미지

}
