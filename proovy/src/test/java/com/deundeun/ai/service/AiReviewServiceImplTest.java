package com.deundeun.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.deundeun.ai.client.AiReviewClient;
import com.deundeun.ai.dto.AiReviewAiResult;
import com.deundeun.ai.dto.AiReviewContext;
import com.deundeun.ai.dto.AiReviewResponse;
import com.deundeun.ai.enums.AiReviewDecision;
import com.deundeun.ai.mapper.AiReviewMapper;
import com.deundeun.ai.mapper.AiReviewRuleMapper;
import com.deundeun.ai.mapper.AiTicketMapper;
import com.deundeun.ai.vo.AiReviewResultVo;
import com.deundeun.ai.vo.AiReviewRuleVo;
import com.deundeun.ai.vo.AiTicketHistoryVo;
import com.deundeun.ai.vo.AiTicketSubscriptionVo;
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
    private AiTicketMapper aiTicketMapper;

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
        lenient().when(aiReviewMapper.isAiReviewEnabledByChallengeId(any())).thenReturn(true);
        lenient().when(aiReviewMapper.existsActiveTicketSubscriptionByHostId(any())).thenReturn(true);
        lenient().when(aiReviewMapper.updateCertificationPostStatus(any(), any())).thenReturn(1);
        lenient().when(aiTicketMapper.findActiveSubscriptionByHostIdForUpdate(any()))
                .thenReturn(AiTicketSubscriptionVo.builder().id(900L).hostId(1L).status("ACTIVE").build());
    }

    @Test
    @DisplayName("방장은 PENDING 인증글의 AI 검수를 요청하고 결과를 저장할 수 있다")
    void review_hostPendingPost_savesResult() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "검수 기준을 충족했습니다.", 0.9);
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
        assertThat(captured.getNewPostStatus()).isEqualTo("APPROVED");
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDecision()).isEqualTo("APPROVED");
        verify(aiReviewMapper).updateCertificationPostStatus(postId, "APPROVED");
        ArgumentCaptor<AiTicketHistoryVo> historyCaptor = ArgumentCaptor.forClass(AiTicketHistoryVo.class);
        verify(aiTicketMapper).insertTicketHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getHostId()).isEqualTo(hostId);
        assertThat(historyCaptor.getValue().getSubscriptionId()).isEqualTo(900L);
        assertThat(historyCaptor.getValue().getType()).isEqualTo("USE");
    }

    @ParameterizedTest
    @EnumSource(AiReviewDecision.class)
    @DisplayName("AI가 반환한 모든 decision enum 값을 저장하고 응답할 수 있다")
    void review_allAiDecisions_savesResult(AiReviewDecision decision) {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(decision, "AI 판단 결과입니다.", confidence(decision));
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
    @DisplayName("신뢰도가 낮은 AI 판정은 실패로 저장하고 게시물을 반려한다")
    void review_autoDecisionBelowThreshold_savesRejected() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "AI confidence below threshold", 0.84);
        AiReviewResultVo savedResult = savedResult(300L, context, rule, aiResult, "REJECTED");
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
        assertThat(captor.getValue().getDecision()).isEqualTo("REJECTED");
        assertThat(captor.getValue().getReason())
                .contains("APPROVED")
                .contains("AI confidence below threshold");
        assertThat(response.getDecision()).isEqualTo("REJECTED");
        verify(aiReviewMapper).updateCertificationPostStatus(postId, "REJECTED");
    }

    @Test
    @DisplayName("Manual review mode keeps certification post pending")
    void review_manualMode_keepsPostPending() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = AiReviewRuleVo.builder()
                .id(1L)
                .hostId(hostId)
                .challengeId(challengeId)
                .ruleText("manual review rule")
                .reviewMode("MANUAL")
                .active(true)
                .build();
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "manual assistance", 0.95);
        AiReviewResultVo savedResult = AiReviewResultVo.builder()
                .id(301L)
                .challengeId(challengeId)
                .hostId(hostId)
                .verificationPostId(postId)
                .reviewMode("MANUAL")
                .decision("APPROVED")
                .confidence(BigDecimal.valueOf(0.95))
                .reason("manual assistance")
                .rawResponse(aiResult.getRawResponse())
                .status("COMPLETED")
                .previousPostStatus("PENDING")
                .newPostStatus("PENDING")
                .build();
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("manual prompt");
        when(aiReviewClient.review("manual prompt", imageUrls)).thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 301L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());
        when(aiReviewMapper.updateAiReviewResultCompleted(any())).thenReturn(1);
        when(aiReviewMapper.findReviewResultById(301L)).thenReturn(savedResult);

        aiReviewService.review(hostId, postId);

        ArgumentCaptor<AiReviewResultVo> captor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).updateAiReviewResultCompleted(captor.capture());
        assertThat(captor.getValue().getNewPostStatus()).isEqualTo("PENDING");
        verify(aiReviewMapper, never()).updateCertificationPostStatus(any(), any());
    }

    @Test
    @DisplayName("AI call failure marks reserved result as failed")
    void review_aiClientFails_marksResultFailed() {
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
        verify(aiReviewMapper).updateAiReviewResultFailed(400L);
        verify(aiTicketMapper, never()).insertTicketHistory(any());
        verify(aiReviewMapper, never()).updateAiReviewResultCompleted(any());
        verify(aiReviewMapper, never()).findReviewResultById(any());
    }

    @Test
    @DisplayName("Failed AI review is reset and retried")
    void review_failedResult_retriesUsingExistingReservation() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = AiReviewRuleVo.builder()
                .id(1L)
                .hostId(hostId)
                .challengeId(challengeId)
                .ruleText("updated retry rule")
                .reviewMode("MANUAL")
                .active(true)
                .build();
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "retry succeeds", 0.9);
        AiReviewResultVo failedResult = AiReviewResultVo.builder()
                .id(401L)
                .verificationPostId(postId)
                .status("FAILED")
                .build();
        AiReviewResultVo savedResult = savedResult(401L, context, rule, aiResult);
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findReviewResultByPostId(postId)).thenReturn(failedResult);
        when(aiReviewMapper.resetFailedAiReviewResultToProcessing(any())).thenReturn(1);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("retry prompt");
        when(aiReviewClient.review("retry prompt", imageUrls)).thenReturn(aiResult);
        when(aiReviewMapper.updateAiReviewResultCompleted(any())).thenReturn(1);
        when(aiReviewMapper.findReviewResultById(401L)).thenReturn(savedResult);

        AiReviewResponse response = aiReviewService.review(hostId, postId);

        assertThat(response.getId()).isEqualTo(401L);
        ArgumentCaptor<AiReviewResultVo> retryCaptor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).resetFailedAiReviewResultToProcessing(retryCaptor.capture());
        assertThat(retryCaptor.getValue().getId()).isEqualTo(401L);
        assertThat(retryCaptor.getValue().getReviewMode()).isEqualTo("MANUAL");
        assertThat(retryCaptor.getValue().getPreviousPostStatus()).isEqualTo("PENDING");
        assertThat(retryCaptor.getValue().getNewPostStatus()).isEqualTo("PENDING");
        verify(aiReviewMapper, never()).insertProcessingAiReviewResult(any());
        verify(aiReviewMapper).updateAiReviewResultCompleted(any());
    }

    @Test
    @DisplayName("Expired ticket before completion prevents use history and completion")
    void review_ticketExpiresBeforeCompletion_failsWithoutUseHistory() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "valid result", 0.9);
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("prompt");
        when(aiReviewClient.review("prompt", imageUrls)).thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 402L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());
        when(aiTicketMapper.findActiveSubscriptionByHostIdForUpdate(hostId)).thenReturn(null);

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_PURCHASE_INVALID_REQUEST);

        verify(aiTicketMapper, never()).insertTicketHistory(any());
        verify(aiReviewMapper, never()).updateAiReviewResultCompleted(any());
        verify(aiReviewMapper).updateAiReviewResultFailed(402L);
    }

    @Test
    @DisplayName("비방장은 AI 검수를 요청할 수 없다")
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
    @DisplayName("PENDING 상태가 아닌 인증글은 AI 검수할 수 없다")
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
    @DisplayName("AI review disabled challenge fails before AI call")
    void review_aiReviewDisabled_failsBeforeAiCall() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.isAiReviewEnabledByChallengeId(challengeId)).thenReturn(false);

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_INVALID_REQUEST);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertProcessingAiReviewResult(any());
    }

    @Test
    @DisplayName("Host without active AI ticket fails before AI call")
    void review_noActiveTicket_failsBeforeAiCall() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.existsActiveTicketSubscriptionByHostId(hostId)).thenReturn(false);

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_PURCHASE_INVALID_REQUEST);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertProcessingAiReviewResult(any());
    }

    @Test
    @DisplayName("이미 AI 검수 결과가 있으면 중복 요청을 거부한다")
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
    @DisplayName("검수 기준이 없으면 AI 검수를 요청할 수 없다")
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

    @Test
    @DisplayName("방장 인증글은 AI 활성화와 티켓 없이 자동 검수된다")
    void reviewSubmittedPost_hostPost_reviewsWithoutAiActivationOrTicket() {
        Long hostId = 1L;
        Long postId = 601L;
        Long challengeId = 60L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "인증 방식을 충족했습니다.", 0.95);

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(any(), any(), any())).thenReturn("host prompt");
        when(aiReviewClient.review("host prompt", List.of("https://example.com/thumb.png")))
                .thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 701L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());
        when(aiReviewMapper.updateAiReviewResultCompleted(any())).thenReturn(1);
        when(aiReviewMapper.findReviewResultById(701L)).thenReturn(
                AiReviewResultVo.builder()
                        .id(701L)
                        .challengeId(challengeId)
                        .hostId(hostId)
                        .verificationPostId(postId)
                        .reviewMode("AUTO")
                        .decision("APPROVED")
                        .confidence(BigDecimal.valueOf(0.95))
                        .reason("인증 방식을 충족했습니다.")
                        .rawResponse(aiResult.getRawResponse())
                        .status("COMPLETED")
                        .previousPostStatus("PENDING")
                        .newPostStatus("APPROVED")
                        .build()
        );

        aiReviewService.reviewSubmittedPost(postId);

        verify(aiReviewMapper).updateCertificationPostStatus(postId, "APPROVED");
        verify(aiReviewMapper, never()).isAiReviewEnabledByChallengeId(any());
        verify(aiReviewMapper, never()).existsActiveTicketSubscriptionByHostId(any());
        verify(aiTicketMapper, never()).findActiveSubscriptionByHostIdForUpdate(any());
        verify(aiTicketMapper, never()).insertTicketHistory(any());
    }

    @Test
    @DisplayName("방장의 티켓이 활성화되면 참가자 인증글을 자동 검수한다")
    void reviewSubmittedPost_participantPostWithTicket_reviewsAutomatically() {
        Long hostId = 1L;
        Long postId = 602L;
        Long challengeId = 60L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        ReflectionTestUtils.setField(context, "authorId", 2L);
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "인증 방식을 충족했습니다.", 0.95);

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.existsActiveTicketSubscriptionByHostId(hostId)).thenReturn(true);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(any(), any(), any())).thenReturn("participant prompt");
        when(aiReviewClient.review("participant prompt", List.of("https://example.com/thumb.png")))
                .thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 702L);
            return 1;
        }).when(aiReviewMapper).insertProcessingAiReviewResult(any());
        when(aiReviewMapper.updateAiReviewResultCompleted(any())).thenReturn(1);
        when(aiReviewMapper.findReviewResultById(702L)).thenReturn(
                AiReviewResultVo.builder()
                        .id(702L)
                        .challengeId(challengeId)
                        .hostId(hostId)
                        .verificationPostId(postId)
                        .reviewMode("AUTO")
                        .decision("APPROVED")
                        .confidence(BigDecimal.valueOf(0.95))
                        .reason("인증 방식을 충족했습니다.")
                        .rawResponse(aiResult.getRawResponse())
                        .status("COMPLETED")
                        .previousPostStatus("PENDING")
                        .newPostStatus("APPROVED")
                        .build()
        );

        aiReviewService.reviewSubmittedPost(postId);

        verify(aiReviewMapper).updateCertificationPostStatus(postId, "APPROVED");
        verify(aiReviewMapper, never()).isAiReviewEnabledByChallengeId(any());
        verify(aiTicketMapper).findActiveSubscriptionByHostIdForUpdate(hostId);
        verify(aiTicketMapper).insertTicketHistory(any());
    }

    @Test
    @DisplayName("티켓이 없는 방장의 참가자 인증글은 자동 검수하지 않는다")
    void reviewSubmittedPost_participantPostWithoutTicket_skipsReview() {
        Long postId = 602L;
        AiReviewContext context = context(postId, 60L, 1L, "PENDING");
        ReflectionTestUtils.setField(context, "authorId", 2L);
        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.existsActiveTicketSubscriptionByHostId(1L)).thenReturn(false);

        aiReviewService.reviewSubmittedPost(postId);

        verify(aiReviewRuleMapper, never()).findAiReviewRuleByChallengeId(any());
        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertProcessingAiReviewResult(any());
    }

    private AiReviewContext context(Long postId, Long challengeId, Long hostId, String status) {
        AiReviewContext context = new AiReviewContext();
        ReflectionTestUtils.setField(context, "verificationPostId", postId);
        ReflectionTestUtils.setField(context, "challengeId", challengeId);
        ReflectionTestUtils.setField(context, "hostId", hostId);
        ReflectionTestUtils.setField(context, "authorId", hostId);
        ReflectionTestUtils.setField(context, "challengeTitle", "아침 운동");
        ReflectionTestUtils.setField(context, "verificationMethod", "운동 사진");
        ReflectionTestUtils.setField(context, "previousPostStatus", status);
        ReflectionTestUtils.setField(context, "postContent", "오늘 운동 완료");
        ReflectionTestUtils.setField(context, "thumbnailUrl", "https://example.com/thumb.png");
        return context;
    }

    private AiReviewRuleVo rule(Long hostId, Long challengeId) {
        return AiReviewRuleVo.builder()
                .id(1L)
                .hostId(hostId)
                .challengeId(challengeId)
                .ruleText("운동 인증 사진인지 확인")
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
            case REJECTED -> 0.92;
            case NEEDS_REVIEW -> 0.43;
        };
    }

    private String expectedDecision(AiReviewDecision decision, double confidence) {
        if (decision == AiReviewDecision.NEEDS_REVIEW || confidence < 0.85) {
            return "REJECTED";
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

