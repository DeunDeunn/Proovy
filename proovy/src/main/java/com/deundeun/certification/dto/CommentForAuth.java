package com.deundeun.certification.dto;

import lombok.Data;

// 권한/뎁스 판별용 댓글 최소 정보 (삭제되지 않은 댓글만 조회됨)
@Data
public class CommentForAuth {
    private Long id;
    private Long postId;
    private Long authorId;              // 댓글 작성자 user_id
    private Long parentCommentId;       // 대댓글이면 부모 id, 최상위면 null
}
