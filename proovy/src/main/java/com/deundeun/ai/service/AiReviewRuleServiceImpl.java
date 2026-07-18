package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiReviewRuleRequest;
import com.deundeun.ai.dto.AiReviewRuleResponse;
import com.deundeun.ai.enums.AiReviewMode;
import com.deundeun.ai.mapper.AiReviewRuleMapper;
import com.deundeun.ai.vo.AiReviewRuleVo;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiReviewRuleServiceImpl implements AiReviewRuleService {

    private final AiReviewRuleMapper aiReviewRuleMapper;

    @Override
    @Transactional(readOnly = true)
    public AiReviewRuleResponse findAiReviewRuleByChallengeId(Long userId, Long challengeId) {
        validateIds(userId, challengeId);
        validateChallengeOwner(userId, challengeId);

        AiReviewRuleVo aiReviewRuleVo = aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId);
        validateFound(aiReviewRuleVo);

        return toResponse(aiReviewRuleVo);
    }

    @Override
    @Transactional
    public AiReviewRuleResponse upsertAiReviewRule(Long userId, Long challengeId, AiReviewRuleRequest request) {
        validateIds(userId, challengeId);
        validateRequest(request);
        Long hostId = validateChallengeOwner(userId, challengeId);
        String reviewMode = normalizeReviewMode(request.getReviewMode());

        AiReviewRuleVo aiReviewRuleVo = AiReviewRuleVo.builder()
                .hostId(hostId)
                .challengeId(challengeId)
                .ruleText(request.getRuleText().trim())
                .reviewMode(reviewMode)
                .build();

        aiReviewRuleMapper.upsertAiReviewRule(aiReviewRuleVo);

        AiReviewRuleVo savedRule = aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId);
        validateFound(savedRule);
        return toResponse(savedRule);
    }

    @Override
    @Transactional
    public AiReviewRuleResponse updateAiReviewModeByChallengeId(Long userId, Long challengeId, String reviewMode) {
        validateIds(userId, challengeId);
        validateChallengeOwner(userId, challengeId);
        String normalizedReviewMode = normalizeReviewMode(reviewMode);

        AiReviewRuleVo aiReviewRuleVo = aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId);
        validateFound(aiReviewRuleVo);

        aiReviewRuleMapper.updateAiReviewModeByChallengeId(challengeId, normalizedReviewMode);

        AiReviewRuleVo savedRule = aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId);
        validateFound(savedRule);
        return toResponse(savedRule);
    }

    private Long validateChallengeOwner(Long userId, Long challengeId) {
        Long hostId = aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId);
        if (hostId == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        if (!hostId.equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return hostId;
    }

    private void validateIds(Long userId, Long challengeId) {
        if (userId == null || challengeId == null) {
            throw new ApiException(ErrorCode.AI_REVIEW_INVALID_REQUEST);
        }
    }

    private void validateRequest(AiReviewRuleRequest request) {
        if (request == null || isBlank(request.getRuleText()) || isBlank(request.getReviewMode())) {
            throw new ApiException(ErrorCode.AI_REVIEW_INVALID_REQUEST);
        }
    }

    private String normalizeReviewMode(String reviewMode) {
        try {
            return AiReviewMode.normalize(reviewMode);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.AI_REVIEW_MODE_INVALID);
        }
    }

    private void validateFound(AiReviewRuleVo aiReviewRuleVo) {
        if (aiReviewRuleVo == null) {
            throw new ApiException(ErrorCode.AI_REVIEW_RULE_NOT_FOUND);
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
