package com.deundeun.certification.mapper;

import com.deundeun.certification.dto.CommentForAuth;
import com.deundeun.certification.dto.CommentRow;
import com.deundeun.certification.dto.CreateCommentSqlParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    // 댓글/대댓글 등록 (id는 useGeneratedKeys로 param.id에 채워짐)
    void insertComment(CreateCommentSqlParam param);

    // 권한/뎁스 판별용 단건 조회 (삭제되지 않은 댓글만). 없으면 null
    CommentForAuth findCommentForAuth(Long commentId);

    // 댓글 내용 수정 (삭제되지 않은 댓글만). 반환=영향받은 행 수
    int updateComment(@Param("commentId") Long commentId, @Param("contents") String contents);

    // 댓글 soft delete (삭제되지 않은 댓글만). 반환=영향받은 행 수
    int softDeleteComment(Long commentId);

    // 최상위 댓글 목록 (커서 무한스크롤·최신순). 대댓글이 달린 삭제 댓글은 껍데기로 포함
    List<CommentRow> findTopLevelComments(@Param("postId") Long postId,
                                          @Param("viewerId") Long viewerId,
                                          @Param("cursor") Long cursor,
                                          @Param("size") int size);

    // 주어진 부모 댓글들의 대댓글 목록 (오래된순). 삭제된 대댓글은 제외
    List<CommentRow> findReplies(@Param("parentIds") List<Long> parentIds,
                                 @Param("viewerId") Long viewerId);

    // 댓글 좋아요 삭제, 0 or 1
    int deleteCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    // 댓글 좋아요 등록, 중복 X / 0 or 1
    int insertCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    // 댓글 좋아요 +1
    void incrementCommentLikeCount(Long commentId);

    // 댓글 좋아요 -1 (0 미만 방지)
    void decrementCommentLikeCount(Long commentId);

    // 댓글 좋아요 집계
    long findCommentLikeCount(Long commentId);

    // 댓글 수 +1 (삭제해도 감소시키지 않음 → 삭제 댓글도 카운트에 포함)
    void incrementCommentCount(Long postId);
}
