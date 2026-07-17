package com.deundeun.certification.dto;

import lombok.Data;

// 인증글 수정 (JSON 파트).
// 대표이미지·추가이미지는 멀티파트 파일(thumbnail/images)로 별도로 받으므로 여기엔 본문만 담는다.
@Data
public class UpdateCertificationPostRequest {
    private String contents;
}
