package com.deundeun.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deundeun.ai.dto.AiTicketActiveResponse;
import com.deundeun.ai.dto.AiTicketPlanResponse;
import com.deundeun.ai.dto.AiTicketPurchaseRequest;
import com.deundeun.ai.dto.AiTicketPurchaseResponse;
import com.deundeun.ai.event.AiTicketActivatedEvent;
import com.deundeun.ai.mapper.AiTicketMapper;
import com.deundeun.ai.vo.AiTicketHistoryVo;
import com.deundeun.ai.vo.AiTicketPlanVo;
import com.deundeun.ai.vo.AiTicketSubscriptionVo;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.service.WalletTicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

@DisplayName("AiTicketService")
@ExtendWith(MockitoExtension.class)
class AiTicketServiceTest {

    @Mock
    private AiTicketMapper aiTicketMapper;

    @Mock
    private WalletTicketService walletTicketService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AiTicketService aiTicketService;

    @Test
    @DisplayName("Expired active subscriptions are updated with expire histories")
    void expireActiveSubscriptions_returnsProcessedCount() {
        when(aiTicketMapper.expireActiveSubscriptions()).thenReturn(3);

        int processedCount = aiTicketService.expireActiveSubscriptions();

        assertThat(processedCount).isEqualTo(3);
        verify(aiTicketMapper).expireActiveSubscriptions();
    }

    @Test
    @DisplayName("active plans are converted to response DTOs")
    void findActivePlans_returnsResponses() {
        AiTicketPlanVo oneDayPlan = activePlan();

        when(aiTicketMapper.findActivePlans()).thenReturn(List.of(oneDayPlan));

        List<AiTicketPlanResponse> responses = aiTicketService.findActivePlans();

        assertThat(responses).hasSize(1);
        AiTicketPlanResponse response = responses.getFirst();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("1 day AI ticket");
        assertThat(response.getDurationDays()).isEqualTo(1);
        assertThat(response.getPrice()).isEqualTo(1000);
        assertThat(response.getDescription()).isEqualTo("Use AI review for 24 hours");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    @DisplayName("active subscription returns current ticket details")
    void findActiveSubscription_existingSubscription_returnsActiveResponse() {
        Long userId = 5L;
        LocalDateTime startedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(6);
        AiTicketSubscriptionVo subscription = AiTicketSubscriptionVo.builder()
                .id(10L)
                .hostId(userId)
                .planId(2L)
                .planName("7 day AI ticket")
                .startedAt(startedAt)
                .expiredAt(expiredAt)
                .status("ACTIVE")
                .build();

        when(aiTicketMapper.findActiveSubscriptionByHostId(userId)).thenReturn(subscription);

        AiTicketActiveResponse response = aiTicketService.findActiveSubscription(userId);

        assertThat(response.isHasActiveTicket()).isTrue();
        assertThat(response.getSubscriptionId()).isEqualTo(10L);
        assertThat(response.getPlanId()).isEqualTo(2L);
        assertThat(response.getPlanName()).isEqualTo("7 day AI ticket");
        assertThat(response.getStartedAt()).isEqualTo(startedAt);
        assertThat(response.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("active subscription returns empty response when no current ticket exists")
    void findActiveSubscription_missingSubscription_returnsEmptyResponse() {
        Long userId = 5L;

        when(aiTicketMapper.findActiveSubscriptionByHostId(userId)).thenReturn(null);

        AiTicketActiveResponse response = aiTicketService.findActiveSubscription(userId);

        assertThat(response.isHasActiveTicket()).isFalse();
        assertThat(response.getSubscriptionId()).isNull();
        assertThat(response.getPlanId()).isNull();
        assertThat(response.getPlanName()).isNull();
        assertThat(response.getStartedAt()).isNull();
        assertThat(response.getExpiredAt()).isNull();
        assertThat(response.getStatus()).isNull();
    }

    @Test
    @DisplayName("purchase creates subscription, deducts cash, and creates history")
    void purchase_activePlan_succeeds() {
        Long userId = 5L;
        AiTicketPlanVo plan = activePlan();

        when(aiTicketMapper.findPlanById(1L)).thenReturn(plan);
        doAnswer(invocation -> {
            AiTicketSubscriptionVo subscription = invocation.getArgument(0);
            ReflectionTestUtils.setField(subscription, "id", 10L);
            return 1;
        }).when(aiTicketMapper).insertSubscription(any());

        AiTicketPurchaseResponse response = aiTicketService.purchase(userId, new AiTicketPurchaseRequest(1L));

        ArgumentCaptor<AiTicketSubscriptionVo> subscriptionCaptor =
                ArgumentCaptor.forClass(AiTicketSubscriptionVo.class);
        verify(aiTicketMapper).insertSubscription(subscriptionCaptor.capture());
        AiTicketSubscriptionVo subscription = subscriptionCaptor.getValue();
        assertThat(subscription.getHostId()).isEqualTo(userId);
        assertThat(subscription.getPlanId()).isEqualTo(1L);
        assertThat(subscription.getPaidPrice()).isEqualTo(1000);
        assertThat(subscription.getStatus()).isEqualTo("ACTIVE");
        assertThat(subscription.getExpiredAt()).isAfter(subscription.getStartedAt());

        verify(walletTicketService).purchase(userId, 1000, 10L);

        ArgumentCaptor<AiTicketHistoryVo> historyCaptor = ArgumentCaptor.forClass(AiTicketHistoryVo.class);
        verify(aiTicketMapper).insertTicketHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getHostId()).isEqualTo(userId);
        assertThat(historyCaptor.getValue().getSubscriptionId()).isEqualTo(10L);
        verify(eventPublisher).publishEvent(new AiTicketActivatedEvent(userId, subscription.getStartedAt()));
        assertThat(historyCaptor.getValue().getType()).isEqualTo("PURCHASE");

        assertThat(response.getSubscriptionId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getPaidPrice()).isEqualTo(1000);
        assertThat(response.getTicketPlan().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("purchase fails when request is invalid")
    void purchase_invalidRequest_fails() {
        assertThatThrownBy(() -> aiTicketService.purchase(1L, new AiTicketPurchaseRequest(null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_PURCHASE_INVALID_REQUEST);

        verify(aiTicketMapper, never()).findPlanById(any());
        verify(walletTicketService, never()).purchase(any(), anyLong(), any());
    }

    @Test
    @DisplayName("purchase fails when user already has active subscription")
    void purchase_activeSubscriptionExists_fails() {
        Long userId = 5L;
        AiTicketSubscriptionVo subscription = AiTicketSubscriptionVo.builder()
                .id(10L)
                .hostId(userId)
                .planId(1L)
                .status("ACTIVE")
                .build();

        when(aiTicketMapper.findActiveSubscriptionByHostId(userId)).thenReturn(subscription);

        assertThatThrownBy(() -> aiTicketService.purchase(userId, new AiTicketPurchaseRequest(1L)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_ALREADY_ACTIVE);

        verify(aiTicketMapper, never()).findPlanById(any());
        verify(aiTicketMapper, never()).insertSubscription(any());
        verify(walletTicketService, never()).purchase(any(), anyLong(), any());
    }

    @Test
    @DisplayName("purchase maps database active subscription conflict to A019")
    void purchase_activeSubscriptionConstraintConflict_failsWithA019() {
        Long userId = 5L;
        AiTicketPlanVo plan = activePlan();

        when(aiTicketMapper.findActiveSubscriptionByHostId(userId)).thenReturn(null);
        when(aiTicketMapper.findPlanById(1L)).thenReturn(plan);
        when(aiTicketMapper.insertSubscription(any()))
                .thenThrow(new DataIntegrityViolationException(
                        "violates exclusion constraint \"ex_ai_ticket_subscriptions_active_period\""
                ));

        assertThatThrownBy(() -> aiTicketService.purchase(userId, new AiTicketPurchaseRequest(1L)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_ALREADY_ACTIVE);

        verify(walletTicketService, never()).purchase(any(), anyLong(), any());
        verify(aiTicketMapper, never()).insertTicketHistory(any());
    }

    @Test
    @DisplayName("purchase fails when plan does not exist")
    void purchase_missingPlan_fails() {
        when(aiTicketMapper.findPlanById(999L)).thenReturn(null);

        assertThatThrownBy(() -> aiTicketService.purchase(1L, new AiTicketPurchaseRequest(999L)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_PLAN_NOT_FOUND);

        verify(aiTicketMapper, never()).insertSubscription(any());
        verify(walletTicketService, never()).purchase(any(), anyLong(), any());
    }

    @Test
    @DisplayName("purchase fails when plan is inactive")
    void purchase_inactivePlan_fails() {
        AiTicketPlanVo inactivePlan = AiTicketPlanVo.builder()
                .id(1L)
                .name("1 day AI ticket")
                .durationDays(1)
                .price(1000)
                .description("Use AI review for 24 hours")
                .active(false)
                .build();
        when(aiTicketMapper.findPlanById(1L)).thenReturn(inactivePlan);

        assertThatThrownBy(() -> aiTicketService.purchase(1L, new AiTicketPurchaseRequest(1L)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_TICKET_PLAN_INACTIVE);

        verify(aiTicketMapper, never()).insertSubscription(any());
        verify(walletTicketService, never()).purchase(any(), anyLong(), any());
    }

    private AiTicketPlanVo activePlan() {
        return AiTicketPlanVo.builder()
                .id(1L)
                .name("1 day AI ticket")
                .durationDays(1)
                .price(1000)
                .description("Use AI review for 24 hours")
                .active(true)
                .build();
    }
}
