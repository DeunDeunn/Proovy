package com.deundeun.ai.service;

import com.deundeun.ai.client.AiReviewClient;
import com.deundeun.ai.dto.AiPageResponse;
import com.deundeun.ai.dto.AiReviewAiResult;
import com.deundeun.ai.dto.AiReviewContext;
import com.deundeun.ai.dto.AiReviewResponse;
import com.deundeun.ai.dto.AiReviewResultItemResponse;
import com.deundeun.ai.enums.AiReviewDecision;
import com.deundeun.ai.mapper.AiReviewMapper;
import com.deundeun.ai.mapper.AiReviewRuleMapper;
import com.deundeun.ai.vo.AiReviewResultVo;
import com.deundeun.ai.vo.AiReviewRuleVo;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReviewServiceImpl implements AiReviewService {

    private static final String COMPLETED = "COMPLETED";
    private static final String PROCESSING = "PROCESSING";
    private static final double AUTO_DECISION_CONFIDENCE_THRESHOLD = 0.85;
    private static final String LOW_CONFIDENCE_REASON_FORMAT =
            "AI 신뢰도가 0.85 미만이라 추가 검증이 필요합니다. 원래 AI 판단: %s. 원래 사유: %s";

    private final AiReviewMapper aiReviewMapper;
    private final AiReviewRuleMapper aiReviewRuleMapper;
    private final AiReviewPromptService aiReviewPromptService;
    private final AiReviewClient aiReviewClient;
    private final TransactionOperations transactionOperations;

    @Override
    public AiReviewResponse review(Long requesterId, Long postId) {
        ReservedReview reservedReview = transactionOperations.execute(status -> reserveReview(requesterId, postId));

        AiReviewContext context = reservedReview.context();
        AiReviewRuleVo rule = reservedReview.rule();
        List<String> imageUrls = buildImageUrls(context, aiReviewMapper.findImageUrlsByPostId(postId));
        String prompt = aiReviewPromptService.createPrompt(context, rule, imageUrls);
        AiReviewAiResult aiResult = aiReviewClient.review(prompt, imageUrls);
        validateAiResult(aiResult);

        return transactionOperations.execute(status -> completeReview(reservedReview.resultId(), context, rule, aiResult));
    }

    @Override
    public AiPageResponse<AiReviewResultItemResponse> findReviewResultsByChallengeId(
            Long requesterId,
            Long challengeId,
            int page,
            int size
    ) {
        validateIds(requesterId, challengeId);
        validateChallengeOwner(requesterId, challengeId);

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        long offset = (long) safePage * safeSize;

        List<AiReviewResultItemResponse> content = aiReviewMapper
                .findReviewResultsByChallengeId(challengeId, safeSize, offset)
                .stream()
                .map(AiReviewResultItemResponse::from)
                .toList();
        long totalElements = aiReviewMapper.countReviewResultsByChallengeId(challengeId);
        return AiPageResponse.of(content, safePage, safeSize, totalElements);
    }

    private ReservedReview reserveReview(Long requesterId, Long postId) {
        validateIds(requesterId, postId);

        AiReviewContext context = aiReviewMapper.findReviewContextByPostId(postId);
        validateContext(context);
        validateHost(requesterId, context.getHostId());
        validatePending(context);

        AiReviewRuleVo rule = aiReviewRuleMapper.findAiReviewRuleByChallengeId(context.getChallengeId());
        validateRule(rule);

        AiReviewResultVo reservation = toProcessingResult(context, rule);
        try {
            aiReviewMapper.insertProcessingAiReviewResult(reservation);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorCode.AI_REVIEW_RESULT_ALREADY_EXISTS);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateReviewConflict(e)) {
                throw new ApiException(ErrorCode.AI_REVIEW_RESULT_ALREADY_EXISTS);
            }
            throw e;
        }
        return new ReservedReview(reservation.getId(), context, rule);
    }

    private AiReviewResponse completeReview(Long resultId, AiReviewContext context, AiReviewRuleVo rule, AiReviewAiResult aiResult) {
        AiReviewResultVo result = toCompletedResult(resultId, context, rule, aiResult);
        if (aiReviewMapper.updateAiReviewResultCompleted(result) == 0) {
            throw new ApiException(ErrorCode.AI_REVIEW_RESULT_ALREADY_EXISTS);
        }

        AiReviewResultVo savedResult = aiReviewMapper.findReviewResultById(resultId);
        return AiReviewResponse.from(savedResult);
    }

    private boolean isDuplicateReviewConflict(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null
                && (message.contains("uq_ai_review_results_verification_post")
                || message.contains("verification_post_id"));
    }

    private void validateIds(Long requesterId, Long postId) {
        if (requesterId == null || postId == null) {
            throw new ApiException(ErrorCode.AI_REVIEW_INVALID_REQUEST);
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

    private void validateChallengeOwner(Long requesterId, Long challengeId) {
        Long hostId = aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId);
        if (hostId == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        validateHost(requesterId, hostId);
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

    private void validateAiResult(AiReviewAiResult aiResult) {
        if (aiResult == null
                || aiResult.getDecision() == null
                || aiResult.getReason() == null
                || aiResult.getReason().isBlank()
                || aiResult.getRawResponse() == null
                || aiResult.getRawResponse().isBlank()
                || aiResult.getConfidence() < 0.0
                || aiResult.getConfidence() > 1.0) {
            throw new ApiException(ErrorCode.GEMINI_RESPONSE_INVALID);
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

    private AiReviewResultVo toProcessingResult(AiReviewContext context, AiReviewRuleVo rule) {
        return AiReviewResultVo.builder()
                .challengeId(context.getChallengeId())
                .hostId(context.getHostId())
                .verificationPostId(context.getVerificationPostId())
                .reviewMode(rule.getReviewMode())
                .status(PROCESSING)
                .previousPostStatus(context.getPreviousPostStatus())
                .newPostStatus(context.getPreviousPostStatus())
                .build();
    }

    private AiReviewResultVo toCompletedResult(Long resultId, AiReviewContext context, AiReviewRuleVo rule, AiReviewAiResult aiResult) {
        AiReviewDecision decision = resolveDecision(aiResult);
        String reason = resolveReason(aiResult, decision);
        return AiReviewResultVo.builder()
                .id(resultId)
                .challengeId(context.getChallengeId())
                .hostId(context.getHostId())
                .verificationPostId(context.getVerificationPostId())
                .reviewMode(rule.getReviewMode())
                .decision(decision.name())
                .confidence(BigDecimal.valueOf(aiResult.getConfidence()))
                .reason(reason)
                .rawResponse(aiResult.getRawResponse())
                .status(COMPLETED)
                .previousPostStatus(context.getPreviousPostStatus())
                .newPostStatus(context.getPreviousPostStatus())
                .build();
    }

    private record ReservedReview(
            Long resultId,
            AiReviewContext context,
            AiReviewRuleVo rule
    ) {
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

    private String resolveReason(AiReviewAiResult aiResult, AiReviewDecision decision) {
        if (decision == AiReviewDecision.NEEDS_REVIEW
                && aiResult.getDecision() != AiReviewDecision.NEEDS_REVIEW
                && aiResult.getConfidence() < AUTO_DECISION_CONFIDENCE_THRESHOLD) {
            return LOW_CONFIDENCE_REASON_FORMAT.formatted(aiResult.getDecision().name(), aiResult.getReason());
        }
        return aiResult.getReason();
    }
}
