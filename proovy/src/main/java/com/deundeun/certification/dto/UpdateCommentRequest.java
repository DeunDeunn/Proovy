package com.deundeun.certification.dto;

import lombok.Data;

// 댓글 수정 요청
@Data
public class UpdateCommentRequest {
    private String contents;            // 수정할 댓글 내용 (필수)
}
