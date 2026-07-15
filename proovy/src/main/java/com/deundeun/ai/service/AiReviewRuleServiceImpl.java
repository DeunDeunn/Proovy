package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiReviewRuleRequest;
import com.deundeun.ai.dto.AiReviewRuleResponse;
import com.deundeun.ai.mapper.AiReviewRuleMapper;
import com.deundeun.ai.vo.AiReviewRuleVo;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiReviewRuleServiceImpl implements AiReviewRuleService {

    private final AiReviewRuleMapper aiReviewRuleMapper;

    @Override
    public AiReviewRuleResponse findAiReviewRuleByChallengeId(Long id, Long challengeId) {
        validateIds(id, challengeId);

        AiReviewRuleVo aiReviewRuleVo = aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId);
        validateFound(aiReviewRuleVo);
        validateOwner(id, aiReviewRuleVo);

        return toResponse(aiReviewRuleVo);
    }

    @Override
    public AiReviewRuleResponse upsertAiReviewRule(Long id, Long challengeId, AiReviewRuleRequest request) {
        validateIds(id, challengeId);
        validateRequest(request);

        AiReviewRuleVo aiReviewRuleVo = AiReviewRuleVo.builder()
                .hostId(id)
                .challengeId(challengeId)
                .ruleText(request.getRuleText())
                .reviewMode(request.getReviewMode())
                .build();

        aiReviewRuleMapper.upsertAiReviewRule(aiReviewRuleVo);

        return findAiReviewRuleByChallengeId(id, challengeId);
    }

    @Override
    public AiReviewRuleResponse updateAiReviewModeByChallengeId(Long id, Long challengeId, String reviewMode) {
        validateIds(id, challengeId);
        if (isBlank(reviewMode)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        AiReviewRuleVo aiReviewRuleVo = aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId);
        validateFound(aiReviewRuleVo);
        validateOwner(id, aiReviewRuleVo);

        aiReviewRuleMapper.updateAiReviewModeByChallengeId(challengeId, reviewMode);

        return findAiReviewRuleByChallengeId(id, challengeId);
    }

    private void validateIds(Long id, Long challengeId) {
        if (id == null || challengeId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRequest(AiReviewRuleRequest request) {
        if (request == null || isBlank(request.getRuleText()) || isBlank(request.getReviewMode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateFound(AiReviewRuleVo aiReviewRuleVo) {
        if (aiReviewRuleVo == null) {
            throw new ApiException(ErrorCode.AI_REVIEW_RULE_NOT_FOUND);
        }
    }

    private void validateOwner(Long id, AiReviewRuleVo aiReviewRuleVo) {
        if (!id.equals(aiReviewRuleVo.getHostId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private AiReviewRuleResponse toResponse(AiReviewRuleVo aiReviewRuleVo) {
        return AiReviewRuleResponse.builder()
                .id(aiReviewRuleVo.getId())
                .challengeId(aiReviewRuleVo.getChallengeId())
                .hostId(aiReviewRuleVo.getHostId())
                .ruleText(aiReviewRuleVo.getRuleText())
                .reviewMode(aiReviewRuleVo.getReviewMode())
                .active(aiReviewRuleVo.isActive())
                .updatedAt(aiReviewRuleVo.getUpdatedAt())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
