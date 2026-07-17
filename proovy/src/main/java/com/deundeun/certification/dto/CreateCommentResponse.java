package com.deundeun.certification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// 생성된 댓글 id를 돌려주는용
@Data
@AllArgsConstructor
public class CreateCommentResponse {
    private Long commentId;   // 생성된 댓글 id
}
