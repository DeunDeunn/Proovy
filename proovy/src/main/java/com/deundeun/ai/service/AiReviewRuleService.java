package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiReviewRuleRequest;
import com.deundeun.ai.dto.AiReviewRuleResponse;

public interface AiReviewRuleService {

    AiReviewRuleResponse findAiReviewRuleByChallengeId(Long id, Long challengeId);

    AiReviewRuleResponse upsertAiReviewRule(
            Long id,
            Long challengeId,
            AiReviewRuleRequest request
    );

    AiReviewRuleResponse updateAiReviewModeByChallengeId(
            Long id,
            Long challengeId,
            String reviewMode
    );
}
