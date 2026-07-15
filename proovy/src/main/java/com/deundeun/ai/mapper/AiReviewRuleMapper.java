package com.deundeun.ai.mapper;

import com.deundeun.ai.vo.AiReviewRuleVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiReviewRuleMapper {

    int upsertAiReviewRule(AiReviewRuleVo aiReviewRuleVo);

    AiReviewRuleVo findAiReviewRuleByChallengeId(@Param("challengeId") Long challengeId);

    int updateAiReviewModeByChallengeId(@Param("challengeId") Long challengeId, @Param("reviewMode") String aiReviewMode);
}
