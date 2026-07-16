package com.deundeun.ai.service;

import com.deundeun.ai.client.AiReviewClient;
import com.deundeun.ai.dto.AiReviewAiResult;
import com.deundeun.ai.dto.AiReviewContext;
import com.deundeun.ai.dto.AiReviewResponse;
import com.deundeun.ai.enums.AiReviewDecision;
import com.deundeun.ai.mapper.AiReviewMapper;
import com.deundeun.ai.mapper.AiReviewRuleMapper;
import com.deundeun.ai.vo.AiReviewResultVo;
import com.deundeun.ai.vo.AiReviewRuleVo;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReviewServiceImpl implements AiReviewService {

    private static final String COMPLETED = "COMPLETED";
    private static final double AUTO_DECISION_CONFIDENCE_THRESHOLD = 0.85;

    private final AiReviewMapper aiReviewMapper;
    private final AiReviewRuleMapper aiReviewRuleMapper;
    private final AiReviewPromptService aiReviewPromptService;
    private final AiReviewClient aiReviewClient;

    @Override
    @Transactional
    public AiReviewResponse review(Long requesterId, Long postId) {
        validateIds(requesterId, postId);

        AiReviewContext context = aiReviewMapper.findReviewContextByPostId(postId);
        validateContext(context);
        validateHost(requesterId, context.getHostId());
        validatePending(context);

        if (aiReviewMapper.existsReviewResultByPostId(postId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        AiReviewRuleVo rule = aiReviewRuleMapper.findAiReviewRuleByChallengeId(context.getChallengeId());
        validateRule(rule);

        List<String> imageUrls = buildImageUrls(context, aiReviewMapper.findImageUrlsByPostId(postId));
        String prompt = aiReviewPromptService.createPrompt(context, rule, imageUrls);
        AiReviewAiResult aiResult = aiReviewClient.review(prompt, imageUrls);

        AiReviewResultVo result = toResult(context, rule, aiResult);
        aiReviewMapper.insertAiReviewResult(result);

        AiReviewResultVo savedResult = aiReviewMapper.findReviewResultById(result.getId());
        return AiReviewResponse.from(savedResult);
    }

    private void validateIds(Long requesterId, Long postId) {
        if (requesterId == null || postId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateContext(AiReviewContext context) {
        if (context == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
    }

    private void validateHost(Long requesterId, Long hostId) {
        if (!requesterId.equals(hostId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private void validatePending(AiReviewContext context) {
        if (!"PENDING".equals(context.getPreviousPostStatus())) {
            throw new ApiException(ErrorCode.NOT_PENDING_POST);
        }
    }

    private void validateRule(AiReviewRuleVo rule) {
        if (rule == null) {
            throw new ApiException(ErrorCode.AI_REVIEW_RULE_NOT_FOUND);
        }
    }

    private List<String> buildImageUrls(AiReviewContext context, List<String> additionalImageUrls) {
        List<String> imageUrls = new ArrayList<>();
        if (context.getThumbnailUrl() != null && !context.getThumbnailUrl().isBlank()) {
            imageUrls.add(context.getThumbnailUrl());
        }
        if (additionalImageUrls != null) {
            imageUrls.addAll(additionalImageUrls);
        }
        return imageUrls;
    }

    private AiReviewResultVo toResult(AiReviewContext context, AiReviewRuleVo rule, AiReviewAiResult aiResult) {
        AiReviewDecision decision = resolveDecision(aiResult);
        return AiReviewResultVo.builder()
                .challengeId(context.getChallengeId())
                .hostId(context.getHostId())
                .reviewImageId(context.getReviewImageId())
                .verificationPostId(context.getVerificationPostId())
                .reviewMode(rule.getReviewMode())
                .decision(decision.name())
                .confidence(BigDecimal.valueOf(aiResult.getConfidence()))
                .reason(aiResult.getReason())
                .rawResponse(aiResult.getRawResponse())
                .status(COMPLETED)
                .previousPostStatus(context.getPreviousPostStatus())
                .newPostStatus(context.getPreviousPostStatus())
                .build();
    }

    private AiReviewDecision resolveDecision(AiReviewAiResult aiResult) {
        if (aiResult.getDecision() == AiReviewDecision.NEEDS_REVIEW) {
            return AiReviewDecision.NEEDS_REVIEW;
        }
        if (aiResult.getConfidence() < AUTO_DECISION_CONFIDENCE_THRESHOLD) {
            return AiReviewDecision.NEEDS_REVIEW;
        }
        return aiResult.getDecision();
    }
}
