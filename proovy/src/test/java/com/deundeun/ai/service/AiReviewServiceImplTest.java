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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private AiReviewServiceImpl aiReviewService;

    @Test
    @DisplayName("방장은 PENDING 인증글에 AI 검수를 요청하고 결과를 저장할 수 있다")
    void review_hostPendingPost_savesResult() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        AiReviewAiResult aiResult = aiResult(AiReviewDecision.APPROVED, "기준을 충족했습니다.", 0.9);
        AiReviewResultVo savedResult = savedResult(100L, context, rule, aiResult);

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.existsReviewResultByPostId(postId)).thenReturn(false);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of("https://example.com/sub.png"));
        when(aiReviewPromptService.createPrompt(any(), any(), any())).thenReturn("prompt");
        when(aiReviewClient.review("prompt", List.of("https://example.com/thumb.png", "https://example.com/sub.png")))
                .thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 100L);
            return 1;
        }).when(aiReviewMapper).insertAiReviewResult(any());
        when(aiReviewMapper.findReviewResultById(100L)).thenReturn(savedResult);

        AiReviewResponse response = aiReviewService.review(hostId, postId);

        ArgumentCaptor<AiReviewResultVo> captor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).insertAiReviewResult(captor.capture());
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
        when(aiReviewMapper.existsReviewResultByPostId(postId)).thenReturn(false);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("prompt");
        when(aiReviewClient.review("prompt", imageUrls)).thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 200L);
            return 1;
        }).when(aiReviewMapper).insertAiReviewResult(any());
        when(aiReviewMapper.findReviewResultById(200L)).thenReturn(savedResult);

        AiReviewResponse response = aiReviewService.review(hostId, postId);

        ArgumentCaptor<AiReviewResultVo> captor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).insertAiReviewResult(captor.capture());
        AiReviewResultVo captured = captor.getValue();
        assertThat(captured.getDecision()).isEqualTo(expectedDecision);
        assertThat(captured.getConfidence()).isEqualByComparingTo(BigDecimal.valueOf(confidence(decision)));
        assertThat(response.getDecision()).isEqualTo(expectedDecision);
        assertThat(response.getConfidence()).isEqualByComparingTo(BigDecimal.valueOf(confidence(decision)));
    }

    @Test
    @DisplayName("APPROVED나 REJECTED라도 confidence가 0.85 미만이면 추가 검증으로 저장한다")
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
        when(aiReviewMapper.existsReviewResultByPostId(postId)).thenReturn(false);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("prompt");
        when(aiReviewClient.review("prompt", imageUrls)).thenReturn(aiResult);
        doAnswer(invocation -> {
            AiReviewResultVo result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 300L);
            return 1;
        }).when(aiReviewMapper).insertAiReviewResult(any());
        when(aiReviewMapper.findReviewResultById(300L)).thenReturn(savedResult);

        AiReviewResponse response = aiReviewService.review(hostId, postId);

        ArgumentCaptor<AiReviewResultVo> captor = ArgumentCaptor.forClass(AiReviewResultVo.class);
        verify(aiReviewMapper).insertAiReviewResult(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("NEEDS_REVIEW");
        assertThat(captor.getValue().getReason())
                .contains("AI 신뢰도가 0.85 미만")
                .contains("APPROVED")
                .contains("AI confidence below threshold");
        assertThat(response.getDecision()).isEqualTo("NEEDS_REVIEW");
    }

    @Test
    @DisplayName("AI 호출에 실패하면 결과를 저장하지 않는다")
    void review_aiClientFails_doesNotSaveResult() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");
        AiReviewRuleVo rule = rule(hostId, challengeId);
        List<String> imageUrls = List.of("https://example.com/thumb.png");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.existsReviewResultByPostId(postId)).thenReturn(false);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(rule);
        when(aiReviewMapper.findImageUrlsByPostId(postId)).thenReturn(List.of());
        when(aiReviewPromptService.createPrompt(context, rule, imageUrls)).thenReturn("prompt");
        when(aiReviewClient.review("prompt", imageUrls)).thenThrow(new IllegalStateException("AI request failed"));

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI request failed");

        verify(aiReviewMapper, never()).insertAiReviewResult(any());
        verify(aiReviewMapper, never()).findReviewResultById(any());
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
        verify(aiReviewMapper, never()).insertAiReviewResult(any());
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
        verify(aiReviewMapper, never()).insertAiReviewResult(any());
    }

    @Test
    @DisplayName("이미 AI 검수 결과가 있으면 중복 요청을 거부한다")
    void review_existingResult_failsBeforeAiCall() {
        Long hostId = 1L;
        Long postId = 10L;
        AiReviewContext context = context(postId, 20L, hostId, "PENDING");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.existsReviewResultByPostId(postId)).thenReturn(true);

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertAiReviewResult(any());
    }

    @Test
    @DisplayName("검수 기준이 없으면 AI 검수를 요청할 수 없다")
    void review_missingRule_failsBeforeAiCall() {
        Long hostId = 1L;
        Long postId = 10L;
        Long challengeId = 20L;
        AiReviewContext context = context(postId, challengeId, hostId, "PENDING");

        when(aiReviewMapper.findReviewContextByPostId(postId)).thenReturn(context);
        when(aiReviewMapper.existsReviewResultByPostId(postId)).thenReturn(false);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(null);

        assertThatThrownBy(() -> aiReviewService.review(hostId, postId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_RULE_NOT_FOUND);

        verify(aiReviewClient, never()).review(any(), any());
        verify(aiReviewMapper, never()).insertAiReviewResult(any());
    }

    private AiReviewContext context(Long postId, Long challengeId, Long hostId, String status) {
        AiReviewContext context = new AiReviewContext();
        ReflectionTestUtils.setField(context, "verificationPostId", postId);
        ReflectionTestUtils.setField(context, "challengeId", challengeId);
        ReflectionTestUtils.setField(context, "hostId", hostId);
        ReflectionTestUtils.setField(context, "reviewImageId", 30L);
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
                .reviewImageId(context.getReviewImageId())
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
