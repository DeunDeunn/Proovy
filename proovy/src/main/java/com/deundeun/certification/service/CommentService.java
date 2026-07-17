package com.deundeun.certification.service;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.CommentForAuth;
import com.deundeun.certification.dto.CommentResponse;
import com.deundeun.certification.dto.CommentRow;
import com.deundeun.certification.dto.CreateCommentRequest;
import com.deundeun.certification.dto.CreateCommentSqlParam;
import com.deundeun.certification.dto.UpdateCommentRequest;
import com.deundeun.certification.enums.CertificationStatus;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.certification.mapper.CommentMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 댓글 도메인 Service.
 * 규칙: 2단계 뎁스(대댓글에 다시 댓글 불가), soft delete("삭제된 댓글입니다"/"수정됨"),
 *      읽기 게이트는 인증글과 동일(CertificationService.assertPostReadable 재사용).
 */
@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final CertificationMapper certificationMapper;
    private final CertificationService certificationService;   // 읽기 게이트 재사용

    // 댓글/대댓글 작성 (읽을 수 있는 APPROVED 글에만)
    @Transactional
    public Long createComment(Long postId, Long userId, CreateCommentRequest request) {
        // 내용 필수
        if (request.getContents() == null || request.getContents().isBlank()) {
            throw new ApiException(ErrorCode.COMMENT_CONTENTS_REQUIRED);
        }

        // 글 존재 + 읽기 권한 (없거나 못 읽으면 POST_NOT_FOUND로 은폐)
        CertificationPostDetailResponse post = certificationMapper.findPostDetail(postId);
        if (post == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        certificationService.assertPostReadable(postId, userId);

        // 댓글은 승인된 글에만
        if (post.getStatus() != CertificationStatus.APPROVED) {
            throw new ApiException(ErrorCode.CANNOT_COMMENT_UNAPPROVED);
        }

        // 대댓글이면: 부모가 존재하고, 같은 글이며, 부모가 최상위여야 함(2단계 제한)
        Long parentCommentId = request.getParentCommentId();
        if (parentCommentId != null) {
            CommentForAuth parent = commentMapper.findCommentForAuth(parentCommentId);
            if (parent == null || !parent.getPostId().equals(postId)) {
                throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
            }
            // 부모가 이미 대댓글이면(대댓글에 또 댓글) → 거부
            if (parent.getParentCommentId() != null) {
                throw new ApiException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }
        }

        CreateCommentSqlParam param = new CreateCommentSqlParam();
        param.setPostId(postId);
        param.setUserId(userId);
        param.setParentCommentId(parentCommentId);
        param.setContents(request.getContents());
        commentMapper.insertComment(param);

        commentMapper.incrementCommentCount(postId);
        return param.getId();
    }

    // 댓글 수정 (작성자 본인만). 수정 후 "수정됨" 표시됨
    @Transactional
    public void updateComment(Long commentId, Long userId, UpdateCommentRequest request) {
        if (request.getContents() == null || request.getContents().isBlank()) {
            throw new ApiException(ErrorCode.COMMENT_CONTENTS_REQUIRED);
        }

        CommentForAuth comment = commentMapper.findCommentForAuth(commentId);
        if (comment == null) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }
        // 수정 권한: 작성자 본인만
        if (!comment.getAuthorId().equals(userId)) {
            throw new ApiException(ErrorCode.NO_COMMENT_PERMISSION);
        }

        // 동시 삭제 방어: 조회 후 다른 요청이 이미 삭제했으면 0행 → 성공 응답 막음
        int updated = commentMapper.updateComment(commentId, request.getContents());
        if (updated != 1) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    // 댓글 삭제 (soft). 권한: 댓글 작성자 · 게시글 작성자 · 관리자 (방장 제외)
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        CommentForAuth comment = commentMapper.findCommentForAuth(commentId);
        if (comment == null) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }

        boolean isCommentAuthor = comment.getAuthorId().equals(userId);
        Long postAuthorId = certificationMapper.findPostAuthorId(comment.getPostId());
        boolean isPostAuthor = postAuthorId != null && postAuthorId.equals(userId);
        boolean isAdmin = certificationMapper.isAdmin(userId) > 0;
        if (!isCommentAuthor && !isPostAuthor && !isAdmin) {
            throw new ApiException(ErrorCode.NO_COMMENT_PERMISSION);
        }

        // 동시 삭제 방어: 실제로 이 요청이 삭제(1행)했을 때만 comment_count 감소
        // (양쪽 요청이 조회를 통과해도 softDelete는 하나만 1행 → 중복 감소로 집계 틀어짐 방지)
        int deleted = commentMapper.softDeleteComment(commentId);
        if (deleted != 1) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }
        commentMapper.decrementCommentCount(comment.getPostId());
    }

    // 댓글 목록 조회 (읽을 수 있는 글) — 최상위 커서 페이징 + 대댓글 중첩
    public List<CommentResponse> getComments(Long postId, Long viewerId, Long cursor, Integer size) {
        CertificationPostDetailResponse post = certificationMapper.findPostDetail(postId);
        if (post == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        certificationService.assertPostReadable(postId, viewerId);

        List<CommentRow> topLevel = commentMapper.findTopLevelComments(postId, cursor, clampSize(size));
        if (topLevel.isEmpty()) {
            return new ArrayList<>();
        }

        // 이번 페이지 최상위 댓글들의 대댓글을 한 번에 조회 후 부모별로 그룹핑
        List<Long> parentIds = topLevel.stream().map(CommentRow::getCommentId).toList();
        Map<Long, List<CommentResponse>> repliesByParent = commentMapper.findReplies(parentIds).stream()
                .collect(Collectors.groupingBy(CommentRow::getParentCommentId,
                        Collectors.mapping(this::toResponse, Collectors.toList())));

        List<CommentResponse> result = new ArrayList<>();
        for (CommentRow row : topLevel) {
            CommentResponse res = toResponse(row);
            res.setReplies(repliesByParent.getOrDefault(row.getCommentId(), new ArrayList<>()));
            result.add(res);
        }
        return result;
    }

    // 평면 행 → 응답 DTO. 삭제된 댓글은 내용·작성자를 감추고 deleted 플래그만 준다.
    private CommentResponse toResponse(CommentRow row) {
        CommentResponse res = new CommentResponse();
        res.setCommentId(row.getCommentId());
        res.setDeleted(row.isDeleted());
        res.setEdited(row.isEdited());
        res.setCreatedAt(row.getCreatedAt());
        if (row.isDeleted()) {
            res.setContents(null);          // 프론트에서 "삭제된 댓글입니다" 표시
        } else {
            res.setAuthorId(row.getAuthorId());
            res.setAuthorNickname(row.getAuthorNickname());
            res.setContents(row.getContents());
        }
        return res;
    }

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private int clampSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
