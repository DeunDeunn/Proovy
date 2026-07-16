package com.deundeun.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.List;

@DisplayName("AiReviewService")
@ExtendWith(MockitoExtension.class)
class AiReviewServiceImplTest {

    @Mock
    private AiReviewMapper aiReviewMapper;

    @Mock
    private AiReviewRuleMapper aiReviewRuleMapper;

    @Mock
    private AiReviewPromptService aiReviewPromptService;

    @Mock
    private AiReviewClient aiReviewClient;

    @Mock
    private TransactionOperations transactionOperations;

    @InjectMocks
    private AiReviewServiceImpl aiReviewService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionOperations).execute(any());
    }

    @Test
    @DisplayName("諛⑹옣? PENDING ?몄쬆湲??AI 寃?섎? ?붿껌?섍퀬 寃곌낵瑜???ν븷 ???덈떎")
    void review_hostPendingPost_savesResult() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "湲곗???異⑹”?덉뒿?덈떎.", 0.9);
        AiReviewResultVo savedResult = savedResult(100L, context, rule, aiResult);

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of("https://example.com/sub.png"));
        when(aiReviewPromptService.createPrompt(any(), any(), any())).thenReturn("prompt");
        when(aiReviewClient.review("prompt", List.of("https://example.com/thumb.png", "https://example.com/sub.png")))
                .thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 100L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());
        when(aiReviewMapper.updateAiReviewResultCompleted(any())).thenReturn(1);
        when(aiReviewMapper.findReviewResultById(100L)).thenReturn(savedResult);

        AiReviewResponse response = aiReviewService.review(hostId, postId);

        ArgumentCaptor<AiReviewResultVo> captor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).updateAiReviewResultCompleted(captor.capture());
        AiReviewResultVo captured = captor.getValue();
        assertThat(captured.getChallengeId()).isEqualTo(challengeId);
        assertThat(captured.getHostId()).isEqualTo(hostId);
        assertThat(captured.getVerificationPostId()).isEqualTo(postId);
        assertThat(captured.getDecision()).isEqualTo("APPROVED");
        assertThat(captured.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDecision()).isEqualTo("APPROVED");
    }

    @ParameterizedTest
    @EnumSource(AiReviewDecision.class)
    @DisplayName("AI媛 諛섑솚??紐⑤뱺 decision enum 媛믪쓣 ??ν븯怨??묐떟?????덈떎")
    void review_allAiDecisions_savesResult(AiReviewDecision decision) {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(decision, "AI ?먮떒 寃곌낵?낅땲??", confidence(decision));
        String expectedDecision = expectedDecision(decision, confidence(decision));
        AiReviewResultVo savedResult = savedResult(200L, context, rule, aiResult, expectedDecision);
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("prompt");
        when(aiReviewClient.review("prompt", imageUrls)).thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 200L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());
        when(aiReviewMapper.updateAiReviewResultCompleted(any())).thenReturn(1);
        when(aiReviewMapper.findReviewResultById(200L)).thenReturn(savedResult);

        AiReviewResponse response = aiReviewService.review(hostId, postId);

        ArgumentCaptor<AiReviewResultVo> captor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).updateAiReviewResultCompleted(captor.capture());
        AiReviewResultVo captured = captor.getValue();
        assertThat(captured.getDecision()).isEqualTo(expectedDecision);
        assertThat(captured.getConfidence()).isEqualByComparingTo(BigDecimal.valueOf(confidence(decision)));
        assertThat(response.getDecision()).isEqualTo(expectedDecision);
        assertThat(response.getConfidence()).isEqualByComparingTo(BigDecimal.valueOf(confidence(decision)));
    }

    @Test
    @DisplayName("low confidence auto decision is saved as NEEDS_REVIEW")
    void review_autoDecisionBelowThreshold_savesNeedsReview() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "AI confidence below threshold", 0.84);
        AiReviewResultVo savedResult = savedResult(300L, context, rule, aiResult, "NEEDS_REVIEW");
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("prompt");
        when(aiReviewClient.review("prompt", imageUrls)).thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 300L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());
        when(aiReviewMapper.updateAiReviewResultCompleted(any())).thenReturn(1);
        when(aiReviewMapper.findReviewResultById(300L)).thenReturn(savedResult);

        AiReviewResponse response = aiReviewService.review(hostId, postId);

        ArgumentCaptor<AiReviewResultVo> captor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).updateAiReviewResultCompleted(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("NEEDS_REVIEW");
        assertThat(captor.getValue().getReason())
                .contains("APPROVED")
                .contains("AI confidence below threshold");
        assertThat(response.getDecision()).isEqualTo("NEEDS_REVIEW");
    }

    @Test
    @DisplayName("AI call failure does not complete reserved result")
    void review_aiClientFails_doesNotSaveResult() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("prompt");
        when(aiReviewClient.review("prompt", imageUrls)).thenThrow(new IllegalStateException("AI request failed"));
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 400L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI request failed");

        verify(aiReviewMapper).insertProcessingAiReviewResult(any());
        verify(aiReviewMapper, never()).updateAiReviewResultCompleted(any());
        verify(aiReviewMapper, never()).findReviewResultById(any());
    }

    @Test
    @DisplayName("鍮꾨갑?μ? AI 寃?섎? ?붿껌?????녿떎")
    void review_nonHost_failsBeforeAiCall() {
        Long requesterId = 2L;
        Long hostId = 1L;
        Long postId = 10L;
        AiReviewContext context = context(postId, 20L, hostId, "PENDING");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);

        assertThatThrownBy(() -> aiReviewService.review(requesterId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertProcessingAiReviewResult(any());
    }

    @Test
    @DisplayName("PENDING ?곹깭媛 ?꾨땶 ?몄쬆湲? AI 寃?섑븷 ???녿떎")
    void review_nonPendingPost_failsBeforeAiCall() {
        Long hostId = 1L;
        Long postId = 10L;
        AiReviewContext context = context(postId, 20L, hostId, "APPROVED");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_PENDING_POST);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertProcessingAiReviewResult(any());
    }

    @Test
    @DisplayName("?대? AI 寃??寃곌낵媛 ?덉쑝硫?以묐났 ?붿껌??嫄곕??쒕떎")
    void review_existingResult_failsBeforeAiCall() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.insertProcessingAiReviewResult(any()))
                .thenThrow(new DuplicateKeyException("uq_ai_review_results_verification_post"));

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_RESULT_ALREADY_EXISTS);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper).insertProcessingAiReviewResult(any());
        verify(aiReviewMapper, never()).updateAiReviewResultCompleted(any());
    }

    @Test
    @DisplayName("non-duplicate reservation data integrity failure is not translated")
    void review_nonDuplicateDataIntegrityFailure_rethrows() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.insertProcessingAiReviewResult(any()))
                .thenThrow(new DataIntegrityViolationException("other constraint"));

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("other constraint");

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper).insertProcessingAiReviewResult(any());
        verify(aiReviewMapper, never()).updateAiReviewResultCompleted(any());
    }

    @Test
    @DisplayName("寃??湲곗????놁쑝硫?AI 寃?섎? ?붿껌?????녿떎")
    void review_missingRule_failsBeforeAiCall() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(null);

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_RULE_NOT_FOUND);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertProcessingAiReviewResult(any());
    }

    @Test
    @DisplayName("invalid AI result does not complete reserved result")
    void review_invalidAiResult_failsBeforeSave() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult invalidAiResult = AiReviewAiResult.builder()
                .decision(AiReviewDecision.APPROVED)
                .reason("")
                .confidence(0.99)
                .rawResponse("{\"decision\":\"APPROVED\",\"reason\":\"\",\"confidence\":0.99}")
                .build();
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("safe prompt");
        when(aiReviewClient.review("safe prompt", imageUrls)).thenReturn(invalidAiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 500L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.GEMINI_RESPONSE_INVALID);

        verify(aiReviewMapper).insertProcessingAiReviewResult(any());
        verify(aiReviewMapper, never()).updateAiReviewResultCompleted(any());
    }

    private AiReviewContext context(Long postId, Long challengeId, Long hostId, String status) {
        AiReviewContext context = new AiReviewContext();
        ReflectionTestUtils.setField(context, "verificationPostId", postId);
        ReflectionTestUtils.setField(context, "challengeId", challengeId);
        ReflectionTestUtils.setField(context, "hostId", hostId);
        ReflectionTestUtils.setField(context, "challengeTitle", "?꾩묠 ?대룞");
        ReflectionTestUtils.setField(context, "verificationMethod", "?대룞 ?ъ쭊");
        ReflectionTestUtils.setField(context, "previousPostStatus", status);
        ReflectionTestUtils.setField(context, "postContent", "?ㅻ뒛 ?대룞 ?꾨즺");
        ReflectionTestUtils.setField(context, "thumbnailUrl", "https://example.com/thumb.png");
        return context;
    }

    private AiReviewRuleVo rule(Long hostId, Long challengeId) {
        return AiReviewRuleVo.builder()
                .id(1L)
                .hostId(hostId)
                .challengeId(challengeId)
                .ruleText("?대룞 ?몄쬆 ?ъ쭊?몄? ?뺤씤")
                .reviewMode("AUTO")
                .active(true)
                .build();
    }

    private AiReviewAiResult aiResult(AiReviewDecision decision, String reason, double confidence) {
        return AiReviewAiResult.builder()
                .decision(decision)
                .reason(reason)
                .confidence(confidence)
                .rawResponse("{\"decision\":\"%s\",\"reason\":\"%s\",\"confidence\":%s}"
                        .formatted(decision.name(), reason, confidence))
                .build();
    }

    private double confidence(AiReviewDecision decision) {
        return switch (decision) {
            case APPROVED -> 0.91;
            case REJECTED -> 0.82;
            case NEEDS_REVIEW -> 0.43;
        };
    }

    private String expectedDecision(AiReviewDecision decision, double confidence) {
        if (decision == AiReviewDecision.NEEDS_REVIEW || confidence < 0.85) {
            return "NEEDS_REVIEW";
        }
        return decision.name();
    }

    private AiReviewResultVo savedResult(Long id, AiReviewContext context, AiReviewRuleVo rule, AiReviewAiResult aiResult) {
        return savedResult(id, context, rule, aiResult, aiResult.getDecision().name());
    }

    private AiReviewResultVo savedResult(
            Long id,
            AiReviewContext context,
            AiReviewRuleVo rule,
            AiReviewAiResult aiResult,
            String decision
    ) {
        return AiReviewResultVo.builder()
                .id(id)
                .challengeId(context.getChallengeId())
                .hostId(context.getHostId())
                .verificationPostId(context.getVerificationPostId())
                .reviewMode(rule.getReviewMode())
                .decision(decision)
                .confidence(BigDecimal.valueOf(aiResult.getConfidence()))
                .reason(aiResult.getReason())
                .rawResponse(aiResult.getRawResponse())
                .status("COMPLETED")
                .previousPostStatus(context.getPreviousPostStatus())
                .newPostStatus(context.getPreviousPostStatus())
                .build();
    }
}

