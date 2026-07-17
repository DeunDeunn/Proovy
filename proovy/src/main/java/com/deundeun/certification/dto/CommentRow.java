package com.deundeun.certification.dto;

import lombok.Data;
import java.time.LocalDateTime;

// 댓글 조회 쿼리의 평면(flat) 행. 서비스에서 트리(CommentResponse)로 조립한다.
@Data
public class CommentRow {
    private Long commentId;
    private Long authorId;
    private String authorNickname;
    private String contents;
    private boolean deleted;            // soft delete 여부
    private boolean edited;             // 수정 여부 (modified_at 존재 & 미삭제)
    private Long parentCommentId;       // 대댓글 그룹핑용
    private LocalDateTime createdAt;
}
