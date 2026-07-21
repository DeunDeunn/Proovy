package com.deundeun.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.CommentForAuth;
import com.deundeun.certification.dto.CommentResponse;
import com.deundeun.certification.dto.CommentRow;
import com.deundeun.certification.dto.CreateCommentRequest;
import com.deundeun.certification.dto.CreateCommentSqlParam;
import com.deundeun.certification.dto.LikeToggleResponse;
import com.deundeun.certification.dto.UpdateCommentRequest;
import com.deundeun.certification.enums.CertificationStatus;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.certification.mapper.CommentMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService - 댓글 CRUD")
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CertificationMapper certificationMapper;
    @Mock
    private CertificationService certificationService;

    @InjectMocks
    private CommentService commentService;

    private static final Long POST_ID = 1L;
    private static final Long USER_ID = 10L;

    private CertificationPostDetailResponse post(CertificationStatus status) {
        CertificationPostDetailResponse p = new CertificationPostDetailResponse();
        p.setPostId(POST_ID);
        p.setAuthorId(99L);
        p.setStatus(status);
        return p;
    }

    private CreateCommentRequest createReq(String contents, Long parentId) {
        CreateCommentRequest r = new CreateCommentRequest();
        r.setContents(contents);
        r.setParentCommentId(parentId);
        return r;
    }

    private CommentForAuth comment(long id, long authorId, Long parentId) {
        CommentForAuth c = new CommentForAuth();
        c.setId(id);
        c.setPostId(POST_ID);
        c.setAuthorId(authorId);
        c.setParentCommentId(parentId);
        return c;
    }

    @Nested
    @DisplayName("createComment")
    class Create {
        @Test
        @DisplayName("[CM-01] 내용이 비면 COMMENT_CONTENTS_REQUIRED")
        void blankContents() {
            assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, createReq("  ", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_CONTENTS_REQUIRED);
        }

        @Test
        @DisplayName("[CM-02] 글이 없으면 POST_NOT_FOUND")
        void postNotFound() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(null);

            assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, createReq("hi", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[CM-03] 미승인 글이면 CANNOT_COMMENT_UNAPPROVED")
        void unapproved() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.PENDING));
            doNothing().when(certificationService).assertPostReadable(POST_ID, USER_ID);

            assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, createReq("hi", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_COMMENT_UNAPPROVED);
        }

        @Test
        @DisplayName("[CM-04] 대댓글 부모가 없거나 다른 글이면 COMMENT_NOT_FOUND")
        void parentMissing() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.APPROVED));
            when(commentMapper.findCommentForAuth(5L)).thenReturn(null);

            assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, createReq("hi", 5L)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("[CM-05] 부모가 이미 대댓글이면 COMMENT_DEPTH_EXCEEDED")
        void depthExceeded() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.APPROVED));
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, 7, 3L)); // 부모의 부모가 있음

            assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, createReq("hi", 5L)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }

        @Test
        @DisplayName("[CM-06] 정상 작성 시 insert + commentCount 증가, 생성 id 반환")
        void success() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.APPROVED));
            doAnswer(inv -> {
                CreateCommentSqlParam p = inv.getArgument(0);
                p.setId(123L);
                return null;
            }).when(commentMapper).insertComment(any(CreateCommentSqlParam.class));

            Long id = commentService.createComment(POST_ID, USER_ID, createReq("hi", null));

            assertThat(id).isEqualTo(123L);
            verify(commentMapper).incrementCommentCount(POST_ID);
        }
    }

    @Nested
    @DisplayName("updateComment")
    class Update {
        @Test
        @DisplayName("[CM-07] 내용이 비면 COMMENT_CONTENTS_REQUIRED")
        void blank() {
            UpdateCommentRequest r = new UpdateCommentRequest();
            r.setContents("");
            assertThatThrownBy(() -> commentService.updateComment(5L, USER_ID, r))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_CONTENTS_REQUIRED);
        }

        @Test
        @DisplayName("[CM-08] 댓글이 없으면 COMMENT_NOT_FOUND")
        void notFound() {
            UpdateCommentRequest r = new UpdateCommentRequest();
            r.setContents("new");
            when(commentMapper.findCommentForAuth(5L)).thenReturn(null);

            assertThatThrownBy(() -> commentService.updateComment(5L, USER_ID, r))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("[CM-09] 작성자가 아니면 NO_COMMENT_PERMISSION")
        void notAuthor() {
            UpdateCommentRequest r = new UpdateCommentRequest();
            r.setContents("new");
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, 999L, null));

            assertThatThrownBy(() -> commentService.updateComment(5L, USER_ID, r))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NO_COMMENT_PERMISSION);
        }

        @Test
        @DisplayName("[CM-10] 작성자 본인이면 수정된다")
        void success() {
            UpdateCommentRequest r = new UpdateCommentRequest();
            r.setContents("new");
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, USER_ID, null));
            when(commentMapper.updateComment(5L, "new")).thenReturn(1);

            commentService.updateComment(5L, USER_ID, r);

            verify(commentMapper).updateComment(5L, "new");
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class Delete {
        @Test
        @DisplayName("[CM-11] 댓글이 없으면 COMMENT_NOT_FOUND")
        void notFound() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(null);

            assertThatThrownBy(() -> commentService.deleteComment(5L, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("[CM-12] 댓글작성자·게시글작성자·관리자 아니면 NO_COMMENT_PERMISSION")
        void noPermission() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, 999L, null));
            when(certificationMapper.findPostAuthorId(POST_ID)).thenReturn(888L);
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> commentService.deleteComment(5L, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NO_COMMENT_PERMISSION);
        }

        @Test
        @DisplayName("[CM-13] 게시글 작성자면 삭제 가능")
        void postAuthorCanDelete() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, 999L, null));
            when(certificationMapper.findPostAuthorId(POST_ID)).thenReturn(USER_ID); // 게시글 작성자 == 요청자
            when(commentMapper.softDeleteComment(5L)).thenReturn(1);

            commentService.deleteComment(5L, USER_ID);

            verify(commentMapper).softDeleteComment(5L);
            verify(commentMapper).decrementCommentCount(POST_ID);
        }

        @Test
        @DisplayName("[CM-14] 댓글 작성자 본인이면 삭제 + commentCount 감소")
        void commentAuthorDeletes() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, USER_ID, null));
            when(commentMapper.softDeleteComment(5L)).thenReturn(1);

            commentService.deleteComment(5L, USER_ID);

            verify(commentMapper).softDeleteComment(5L);
            verify(commentMapper).decrementCommentCount(POST_ID);
        }
    }

    @Nested
    @DisplayName("toggleCommentLike")
    class Like {
        @Test
        @DisplayName("[CM-15] 댓글이 없으면 COMMENT_NOT_FOUND")
        void commentNotFound() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(null);

            assertThatThrownBy(() -> commentService.toggleCommentLike(5L, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("[CM-16] 미승인 글의 댓글에는 좋아요할 수 없다")
        void unapprovedPost() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, 99L, null));
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.PENDING));
            doNothing().when(certificationService).assertPostReadable(POST_ID, USER_ID);

            assertThatThrownBy(() -> commentService.toggleCommentLike(5L, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_LIKE_COMMENT_UNAPPROVED);
        }

        @Test
        @DisplayName("[CM-17] 좋아요 등록 시 liked=true와 증가한 개수를 반환한다")
        void likeOn() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, 99L, null));
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.APPROVED));
            doNothing().when(certificationService).assertPostReadable(POST_ID, USER_ID);
            when(commentMapper.deleteCommentLike(5L, USER_ID)).thenReturn(0);
            when(commentMapper.insertCommentLike(5L, USER_ID)).thenReturn(1);
            when(commentMapper.findCommentLikeCount(5L)).thenReturn(1L);

            LikeToggleResponse result = commentService.toggleCommentLike(5L, USER_ID);

            assertThat(result.isLiked()).isTrue();
            assertThat(result.getLikeCount()).isEqualTo(1L);
            verify(commentMapper).incrementCommentLikeCount(5L);
        }

        @Test
        @DisplayName("[CM-18] 좋아요 취소 시 liked=false와 감소한 개수를 반환한다")
        void likeOff() {
            when(commentMapper.findCommentForAuth(5L)).thenReturn(comment(5, 99L, null));
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.APPROVED));
            doNothing().when(certificationService).assertPostReadable(POST_ID, USER_ID);
            when(commentMapper.deleteCommentLike(5L, USER_ID)).thenReturn(1);
            when(commentMapper.findCommentLikeCount(5L)).thenReturn(0L);

            LikeToggleResponse result = commentService.toggleCommentLike(5L, USER_ID);

            assertThat(result.isLiked()).isFalse();
            assertThat(result.getLikeCount()).isZero();
            verify(commentMapper).decrementCommentLikeCount(5L);
        }
    }

    @Nested
    @DisplayName("getComments")
    class Get {
        @Test
        @DisplayName("[CM-15] 글이 없으면 POST_NOT_FOUND")
        void postNotFound() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(null);

            assertThatThrownBy(() -> commentService.getComments(POST_ID, USER_ID, null, 20))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[CM-16] 최상위 댓글이 없으면 빈 리스트 (대댓글 조회 안 함)")
        void empty() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.APPROVED));
            when(commentMapper.findTopLevelComments(eq(POST_ID), eq(USER_ID), any(), anyInt()))
                    .thenReturn(List.of());

            List<CommentResponse> result = commentService.getComments(POST_ID, USER_ID, null, 20);

            assertThat(result).isEmpty();
            verify(commentMapper, never()).findReplies(any(), any());
        }

        @Test
        @DisplayName("[CM-17] 대댓글을 부모별로 중첩하고 삭제 댓글은 내용을 숨긴다")
        void nestedAndDeletedHidden() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(post(CertificationStatus.APPROVED));

            CommentRow top = new CommentRow();
            top.setCommentId(100L);
            top.setAuthorId(10L);
            top.setAuthorNickname("nick");
            top.setContents("top");
            top.setCreatedAt(LocalDateTime.now());
            when(commentMapper.findTopLevelComments(eq(POST_ID), eq(USER_ID), any(), anyInt()))
                    .thenReturn(List.of(top));

            CommentRow reply = new CommentRow();
            reply.setCommentId(200L);
            reply.setParentCommentId(100L);
            reply.setDeleted(true);              // 삭제된 대댓글
            reply.setContents("secret");
            reply.setCreatedAt(LocalDateTime.now());
            when(commentMapper.findReplies(List.of(100L), USER_ID)).thenReturn(List.of(reply));

            List<CommentResponse> result = commentService.getComments(POST_ID, USER_ID, null, 20);

            assertThat(result).hasSize(1);
            CommentResponse parent = result.get(0);
            assertThat(parent.getContents()).isEqualTo("top");
            assertThat(parent.getReplies()).hasSize(1);
            CommentResponse child = parent.getReplies().get(0);
            assertThat(child.isDeleted()).isTrue();
            assertThat(child.getContents()).isNull();   // 삭제 댓글은 내용 숨김
        }
    }
}
