package com.deundeun.ai.mapper;

import com.deundeun.ai.dto.AiReviewContext;
import com.deundeun.ai.vo.AiReviewResultVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiReviewMapper {

    AiReviewContext findReviewContextByPostId(@Param("postId") Long postId);

    List<String> findImageUrlsByPostId(@Param("postId") Long postId);

    boolean existsReviewResultByPostId(@Param("postId") Long postId);

    int insertAiReviewResult(AiReviewResultVo result);

    AiReviewResultVo findReviewResultById(@Param("id") Long id);
}
