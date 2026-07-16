package com.deundeun.certification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// 좋아요 토글 (한번 누르면 좋아요 두번 누르면 취소(
@Data
@AllArgsConstructor
public class LikeToggleResponse {
    private boolean liked;    // 토글 후 내가 이 글을 좋아요 한 상태인지
    private long likeCount;   // 좋아요집계
}
