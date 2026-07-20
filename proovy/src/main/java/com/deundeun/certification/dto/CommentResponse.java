package com.deundeun.certification.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

// 댓글 1개 (조회 응답). 최상위 댓글은 replies에 대댓글 목록을 중첩해서 담는다.
@Data
public class CommentResponse {
    private Long commentId;
    private Long authorId;
    private String authorNickname;
    private String authorProfileImageUrl;
    private boolean authorBadgeApproved;
    private String contents;            // 삭제된 댓글이면 null (프론트에서 "삭제된 댓글입니다" 표시)
    private long likeCount;
    private boolean liked;              // 현재 로그인 사용자의 좋아요 여부
    private boolean deleted;            // soft delete 여부 → "삭제된 댓글입니다"
    private boolean edited;             // 수정 여부 → "수정됨"
    private LocalDateTime createdAt;
    private List<CommentResponse> replies;  // 대댓글 목록 (최상위 댓글에만 채움, 대댓글은 null)
}
