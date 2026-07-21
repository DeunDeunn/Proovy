package com.deundeun.certification.dto;

import lombok.Data;

import java.util.List;

// 인증글 수정 (JSON 파트).
// 대표이미지·추가이미지는 멀티파트 파일(thumbnail/images)로 별도로 받는다.
// 기존 이미지를 그대로 유지하는 경우엔 파일을 다시 올리지 않고 아래 플래그/URL로 표현한다.
@Data
public class UpdateCertificationPostRequest {
    private String contents;

    // 대표이미지를 기존 것 그대로 둘지 여부. true면 thumbnail 파일 없이 보낸다.
    private boolean keepThumbnail;

    // 유지할 기존 추가이미지 URL 목록(순서 그대로). 새 추가이미지는 images 파일로 보낸다.
    private List<String> keptImageUrls;
}
