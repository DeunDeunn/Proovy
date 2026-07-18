package com.deundeun.certification.controller;

import com.deundeun.certification.dto.CommentResponse;
import com.deundeun.certification.dto.CreateCommentRequest;
import com.deundeun.certification.dto.CreateCommentResponse;
import com.deundeun.certification.dto.UpdateCommentRequest;
import com.deundeun.certification.service.CommentService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;

    // 댓글/대댓글 작성 (읽을 수 있는 APPROVED 글에만)
    @PostMapping("/api/v1/certification-post/{postId}/comments")
    public ApiResponse<CreateCommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request) {
        Long userId = CurrentUser.getUserId();
        Long commentId = commentService.createComment(postId, userId, request);
        return ApiResponse.success(new CreateCommentResponse(commentId));
    }

    // 댓글 목록 조회 (최상위 커서 무한스크롤·최신순 + 대댓글 중첩)
    @GetMapping("/api/v1/certification-post/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(commentService.getComments(postId, viewerId, cursor, size));
    }

    // 댓글 수정 (작성자 본인만)
    @PutMapping("/api/v1/comments/{commentId}")
    public ApiResponse<Void> updateComment(
            @PathVariable Long commentId,
            @RequestBody UpdateCommentRequest request) {
        Long userId = CurrentUser.getUserId();
        commentService.updateComment(commentId, userId, request);
        return ApiResponse.success(null);
    }

    // 댓글 삭제 (soft) — 댓글 작성자·게시글 작성자·관리자
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = CurrentUser.getUserId();
        commentService.deleteComment(commentId, userId);
        return ApiResponse.success(null);
    }
}
