package com.deundeun.certification.dto;

import lombok.Data;
import java.util.List;

// 인증글 수정
@Data
public class UpdateCertificationPostRequest {
    private String contents;
    private String thumbnailImage;    // 대표 이미지 URL (필수)
    private List<String> imageList;   // 최종 유지할 추가 이미지 URL들 (최대 3장)
}
