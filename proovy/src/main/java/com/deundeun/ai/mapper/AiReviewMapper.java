package com.deundeun.ai.mapper;

import com.deundeun.ai.dto.AiReviewContext;
import com.deundeun.ai.vo.AiReviewResultVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiReviewMapper {

    AiReviewContext findReviewContextByPostId(@Param("postId") Long postId);

    boolean isAiReviewEnabledByChallengeId(@Param("challengeId") Long challengeId);

    boolean existsActiveTicketSubscriptionByHostId(@Param("hostId") Long hostId);

    List<String> findImageUrlsByPostId(@Param("postId") Long postId);

    List<Long> findPendingParticipantPostIdsByHostId(@Param("hostId") Long hostId);

    int insertProcessingAiReviewResult(AiReviewResultVo result);

    AiReviewResultVo findReviewResultByPostId(@Param("postId") Long postId);

    int resetFailedAiReviewResultToProcessing(AiReviewResultVo result);

    int updateAiReviewResultFailed(@Param("id") Long id);

    int updateCertificationPostStatus(
            @Param("postId") Long postId,
            @Param("newStatus") String newStatus
    );

    int updateAiReviewResultCompleted(AiReviewResultVo result);

    AiReviewResultVo findReviewResultById(@Param("id") Long id);

    List<AiReviewResultVo> findReviewResultsByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    long countReviewResultsByChallengeId(@Param("challengeId") Long challengeId);
}
