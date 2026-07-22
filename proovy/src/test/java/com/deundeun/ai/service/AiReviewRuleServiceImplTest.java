package com.deundeun.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deundeun.ai.dto.AiReviewRuleRequest;
import com.deundeun.ai.dto.AiReviewRuleResponse;
import com.deundeun.ai.mapper.AiReviewRuleMapper;
import com.deundeun.ai.mapper.AiTicketMapper;
import com.deundeun.ai.vo.AiReviewRuleVo;
import com.deundeun.ai.vo.AiTicketSubscriptionVo;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("AiReviewRuleService")
@ExtendWith(MockitoExtension.class)
class AiReviewRuleServiceImplTest {

    @Mock
    private AiReviewRuleMapper aiReviewRuleMapper;

    @Mock
    private AiTicketMapper aiTicketMapper;

    @InjectMocks
    private AiReviewRuleServiceImpl aiReviewRuleService;

    @Test
    @DisplayName("방장은 AI 검수 규칙을 생성하거나 수정할 수 있다")
    void upsertAiReviewRule_host_succeeds() {
        Long hostId = 1L;
        Long challengeId = 10L;
        AiReviewRuleRequest request = request("auto", "  인증 사진 기준을 확인한다.  ");
        AiReviewRuleVo savedRule = rule(100L, hostId, challengeId, "인증 사진 기준을 확인한다.", "AUTO");

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(hostId);
        when(aiTicketMapper.findActiveSubscriptionByHostId(hostId))
                .thenReturn(AiTicketSubscriptionVo.builder().id(1L).hostId(hostId).build());
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(savedRule);

        AiReviewRuleResponse response = aiReviewRuleService.upsertAiReviewRule(hostId, challengeId, request);

        ArgumentCaptor<AiReviewRuleVo> captor = ArgumentCaptor.forClass(AiReviewRuleVo.class);
        verify(aiReviewRuleMapper).upsertAiReviewRule(captor.capture());
        verify(aiReviewRuleMapper).updateChallengeAiReviewEnabled(challengeId, true);
        AiReviewRuleVo capturedRule = captor.getValue();
        assertThat(capturedRule.getHostId()).isEqualTo(hostId);
        assertThat(capturedRule.getChallengeId()).isEqualTo(challengeId);
        assertThat(capturedRule.getRuleText()).isEqualTo("인증 사진 기준을 확인한다.");
        assertThat(capturedRule.getReviewMode()).isEqualTo("AUTO");
        assertThat(response.getReviewMode()).isEqualTo("AUTO");
    }

    @Test
    @DisplayName("활성 티켓이 없으면 AI 검수를 활성화할 수 없다")
    void upsertAiReviewRule_withoutActiveTicket_failsBeforeWrite() {
        Long hostId = 1L;
        Long challengeId = 10L;
        AiReviewRuleRequest request = request("AUTO", "인증 사진 기준을 확인한다.");

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(hostId);
        when(aiTicketMapper.findActiveSubscriptionByHostId(hostId)).thenReturn(null);

        assertThatThrownBy(() -> aiReviewRuleService.upsertAiReviewRule(hostId, challengeId, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_PURCHASE_INVALID_REQUEST);

        verify(aiReviewRuleMapper, never()).upsertAiReviewRule(any());
        verify(aiReviewRuleMapper, never()).updateChallengeAiReviewEnabled(challengeId, true);
    }

    @Test
    @DisplayName("방장은 AI 검수를 비활성화할 수 있다")
    void deactivateAiReview_host_succeeds() {
        Long hostId = 1L;
        Long challengeId = 10L;

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(hostId);

        aiReviewRuleService.deactivateAiReview(hostId, challengeId);

        verify(aiReviewRuleMapper).deactivateAiReviewRuleByChallengeId(challengeId);
        verify(aiReviewRuleMapper).updateChallengeAiReviewEnabled(challengeId, false);
    }

    @Test
    @DisplayName("비방장은 AI 검수 규칙을 생성할 수 없다")
    void upsertAiReviewRule_nonHost_failsBeforeWrite() {
        Long userId = 2L;
        Long hostId = 1L;
        Long challengeId = 10L;
        AiReviewRuleRequest request = request("AUTO", "인증 사진 기준을 확인한다.");

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(hostId);

        assertThatThrownBy(() -> aiReviewRuleService.upsertAiReviewRule(userId, challengeId, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(aiReviewRuleMapper, never()).upsertAiReviewRule(any());
    }

    @Test
    @DisplayName("비방장은 기존 AI 검수 모드를 수정할 수 없다")
    void updateAiReviewMode_nonHost_failsBeforeWrite() {
        Long userId = 2L;
        Long hostId = 1L;
        Long challengeId = 10L;

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(hostId);

        assertThatThrownBy(() -> aiReviewRuleService.updateAiReviewModeByChallengeId(userId, challengeId, "MANUAL"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(aiReviewRuleMapper, never()).updateAiReviewModeByChallengeId(challengeId, "MANUAL");
    }

    @Test
    @DisplayName("AUTO_DECISION 요청 값은 기존 AUTO 모드로 정규화해 수정한다")
    void updateAiReviewMode_autoDecisionAlias_succeeds() {
        Long hostId = 1L;
        Long challengeId = 10L;
        AiReviewRuleVo savedRule = rule(100L, hostId, challengeId, "인증 사진 기준을 확인한다.", "AUTO");

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(hostId);
        when(aiReviewRuleMapper.findAiReviewRuleByChallengeId(challengeId)).thenReturn(savedRule);

        AiReviewRuleResponse response = aiReviewRuleService.updateAiReviewModeByChallengeId(
                hostId,
                challengeId,
                "AUTO_DECISION"
        );

        verify(aiReviewRuleMapper).updateAiReviewModeByChallengeId(challengeId, "AUTO");
        assertThat(response.getReviewMode()).isEqualTo("AUTO");
    }

    @Test
    @DisplayName("존재하지 않는 챌린지는 검수 규칙을 생성할 수 없다")
    void upsertAiReviewRule_missingChallenge_fails() {
        Long userId = 1L;
        Long challengeId = 10L;
        AiReviewRuleRequest request = request("AUTO", "인증 사진 기준을 확인한다.");

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(null);

        assertThatThrownBy(() -> aiReviewRuleService.upsertAiReviewRule(userId, challengeId, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHALLENGE_NOT_FOUND);

        verify(aiReviewRuleMapper, never()).upsertAiReviewRule(any());
    }

    @Test
    @DisplayName("허용되지 않은 검수 모드는 거부한다")
    void upsertAiReviewRule_invalidReviewMode_fails() {
        Long hostId = 1L;
        Long challengeId = 10L;
        AiReviewRuleRequest request = request("UNKNOWN", "인증 사진 기준을 확인한다.");

        when(aiReviewRuleMapper.findChallengeHostIdByChallengeId(challengeId)).thenReturn(hostId);

        assertThatThrownBy(() -> aiReviewRuleService.upsertAiReviewRule(hostId, challengeId, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_REVIEW_MODE_INVALID);

        verify(aiReviewRuleMapper, never()).upsertAiReviewRule(any());
    }

    private AiReviewRuleRequest request(String reviewMode, String ruleText) {
        AiReviewRuleRequest request = new TestAiReviewRuleRequest();
        ReflectionTestUtils.setField(request, "reviewMode", reviewMode);
        ReflectionTestUtils.setField(request, "ruleText", ruleText);
        return request;
    }

    private AiReviewRuleVo rule(Long id, Long hostId, Long challengeId, String ruleText, String reviewMode) {
        return AiReviewRuleVo.builder()
                .id(id)
                .hostId(hostId)
                .challengeId(challengeId)
                .ruleText(ruleText)
                .reviewMode(reviewMode)
                .active(true)
                .build();
    }

    private static class TestAiReviewRuleRequest extends AiReviewRuleRequest {
    }
}
