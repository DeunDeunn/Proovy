package com.deundeun.certification.dto;

import lombok.Data;

import java.util.List;

//인증글 등록 데이터.
@Data
public class CreateCertificationPostRequest {

    //선택한 챌린지, 본문글, 대표인증이미지(썸네일이 될), 추가 이미지 (최대 3장)

    private String contents;        // 본문 글
    private String thumbnailImage;  // 대표 이미지 (썸네일, 1장 필수 인증용)
    private List<String> imageList; // 추가 이미지 (최대 3장)

}
