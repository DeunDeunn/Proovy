package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiTicketPlanResponse;
import com.deundeun.ai.dto.AiTicketPurchaseRequest;
import com.deundeun.ai.dto.AiTicketPurchaseResponse;
import com.deundeun.ai.mapper.AiTicketMapper;
import com.deundeun.ai.vo.AiTicketHistoryVo;
import com.deundeun.ai.vo.AiTicketPlanVo;
import com.deundeun.ai.vo.AiTicketSubscriptionVo;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.service.WalletTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTicketService {

    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String TICKET_HISTORY_TYPE_PURCHASE = "PURCHASE";

    private final AiTicketMapper aiTicketMapper;
    private final WalletTicketService walletTicketService;

    @Transactional(readOnly = true)
    public List<AiTicketPlanResponse> findActivePlans() {
        return aiTicketMapper.findActivePlans().stream()
                .map(AiTicketPlanResponse::from)
                .toList();
    }

    @Transactional
    public AiTicketPurchaseResponse purchase(Long userId, AiTicketPurchaseRequest request) {
        validatePurchaseRequest(userId, request);

        AiTicketPlanVo plan = aiTicketMapper.findPlanById(request.getPlanId());
        validatePlan(plan);

        LocalDateTime startedAt = LocalDateTime.now();
        AiTicketSubscriptionVo subscription = AiTicketSubscriptionVo.builder()
                .hostId(userId)
                .planId(plan.getId())
                .paidPrice(plan.getPrice())
                .startedAt(startedAt)
                .expiredAt(startedAt.plusDays(plan.getDurationDays()))
                .status(SUBSCRIPTION_STATUS_ACTIVE)
                .build();

        aiTicketMapper.insertSubscription(subscription);
        walletTicketService.purchase(userId, plan.getPrice(), subscription.getId());
        aiTicketMapper.insertTicketHistory(AiTicketHistoryVo.builder()
                .hostId(userId)
                .subscriptionId(subscription.getId())
                .type(TICKET_HISTORY_TYPE_PURCHASE)
                .build());

        return AiTicketPurchaseResponse.from(subscription, plan);
    }

    private void validatePurchaseRequest(Long userId, AiTicketPurchaseRequest request) {
        if (userId == null || request == null || request.getPlanId() == null) {
            throw new ApiException(ErrorCode.AI_TICKET_PURCHASE_INVALID_REQUEST);
        }
    }

    private void validatePlan(AiTicketPlanVo plan) {
        if (plan == null) {
            throw new ApiException(ErrorCode.AI_TICKET_PLAN_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new ApiException(ErrorCode.AI_TICKET_PLAN_INACTIVE);
        }
    }
}
