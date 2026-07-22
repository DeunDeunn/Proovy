package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiTicketActiveResponse;
import com.deundeun.ai.dto.AiPageResponse;
import com.deundeun.ai.dto.AiTicketHistoryItemResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTicketService {

    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String TICKET_HISTORY_TYPE_PURCHASE = "PURCHASE";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final AiTicketMapper aiTicketMapper;
    private final WalletTicketService walletTicketService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AiTicketPlanResponse> findActivePlans() {
        return aiTicketMapper.findActivePlans().stream()
                .map(AiTicketPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiTicketActiveResponse findActiveSubscription(Long userId) {
        validateUserId(userId);

        AiTicketSubscriptionVo subscription = aiTicketMapper.findActiveSubscriptionByHostId(userId);
        if (subscription == null) {
            return AiTicketActiveResponse.empty();
        }
        return AiTicketActiveResponse.active(subscription);
    }

    @Transactional(readOnly = true)
    public AiPageResponse<AiTicketHistoryItemResponse> findTicketHistories(
            Long userId,
            String type,
            int page,
            int size
    ) {
        validateUserId(userId);
        String historyType = normalizeHistoryType(type);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int maxPage = Integer.MAX_VALUE / safeSize;
        if (safePage > maxPage) {
            safePage = maxPage;
        }
        long offset = (long) safePage * safeSize;

        List<AiTicketHistoryItemResponse> content = aiTicketMapper
                .findTicketHistoriesByHostId(userId, historyType, safeSize, offset)
                .stream()
                .map(AiTicketHistoryItemResponse::from)
                .toList();
        long totalElements = aiTicketMapper.countTicketHistoriesByHostId(userId, historyType);
        return AiPageResponse.of(content, safePage, safeSize, totalElements);
    }

    @Transactional
    public AiTicketPurchaseResponse purchase(Long userId, AiTicketPurchaseRequest request) {
        validatePurchaseRequest(userId, request);
        validateNoActiveSubscription(userId);

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

        insertSubscription(subscription);
        walletTicketService.purchase(userId, plan.getPrice(), subscription.getId());
        aiTicketMapper.insertTicketHistory(AiTicketHistoryVo.builder()
                .hostId(userId)
                .subscriptionId(subscription.getId())
                .type(TICKET_HISTORY_TYPE_PURCHASE)
                .build());
        eventPublisher.publishEvent(new AiTicketActivatedEvent(userId, startedAt));

        return AiTicketPurchaseResponse.from(subscription, plan);
    }

    @Transactional
    public int expireActiveSubscriptions() {
        return aiTicketMapper.expireActiveSubscriptions();
    }

    private void insertSubscription(AiTicketSubscriptionVo subscription) {
        try {
            aiTicketMapper.insertSubscription(subscription);
        } catch (DataIntegrityViolationException e) {
            if (isActiveSubscriptionConflict(e)) {
                throw new ApiException(ErrorCode.AI_TICKET_ALREADY_ACTIVE);
            }
            throw e;
        }
    }

    private void validatePurchaseRequest(Long userId, AiTicketPurchaseRequest request) {
        if (userId == null || request == null || request.getPlanId() == null) {
            throw new ApiException(ErrorCode.AI_TICKET_PURCHASE_INVALID_REQUEST);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.AI_TICKET_PURCHASE_INVALID_REQUEST);
        }
    }

    private void validateNoActiveSubscription(Long userId) {
        if (aiTicketMapper.findActiveSubscriptionByHostId(userId) != null) {
            throw new ApiException(ErrorCode.AI_TICKET_ALREADY_ACTIVE);
        }
    }

    private String normalizeHistoryType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return switch (type.trim().toUpperCase()) {
            case "PURCHASED", "PURCHASE" -> "PURCHASE";
            case "USED", "USE" -> "USE";
            case "EXPIRED", "EXPIRE" -> "EXPIRE";
            case "REFUNDED", "REFUND" -> "REFUND";
            default -> throw new ApiException(ErrorCode.AI_TICKET_HISTORY_TYPE_INVALID);
        };
    }

    private boolean isActiveSubscriptionConflict(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("ex_ai_ticket_subscriptions_active_period");
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
