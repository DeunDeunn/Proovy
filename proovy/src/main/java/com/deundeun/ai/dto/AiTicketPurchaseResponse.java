package com.deundeun.ai.dto;

import com.deundeun.ai.vo.AiTicketPlanVo;
import com.deundeun.ai.vo.AiTicketSubscriptionVo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AiTicketPurchaseResponse {

    private Long subscriptionId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private Integer paidPrice;
    private PurchasedTicketPlanResponse ticketPlan;

    public static AiTicketPurchaseResponse from(AiTicketSubscriptionVo subscription, AiTicketPlanVo plan) {
        return AiTicketPurchaseResponse.builder()
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .paidPrice(subscription.getPaidPrice())
                .ticketPlan(PurchasedTicketPlanResponse.from(plan))
                .build();
    }

    @Getter
    @Builder
    public static class PurchasedTicketPlanResponse {

        private Long id;
        private String name;
        private Integer durationDays;
        private Integer price;

        private static PurchasedTicketPlanResponse from(AiTicketPlanVo plan) {
            return PurchasedTicketPlanResponse.builder()
                    .id(plan.getId())
                    .name(plan.getName())
                    .durationDays(plan.getDurationDays())
                    .price(plan.getPrice())
                    .build();
        }
    }
}
