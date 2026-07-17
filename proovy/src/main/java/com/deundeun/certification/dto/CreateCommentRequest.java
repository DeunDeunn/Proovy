package com.deundeun.certification.dto;

import lombok.Data;

// 댓글/대댓글 작성 요청
@Data
public class CreateCommentRequest {
    private String contents;            // 댓글 내용 (필수)
    private Long parentCommentId;       // 대댓글이면 부모 댓글 id, 최상위 댓글이면 null
}
