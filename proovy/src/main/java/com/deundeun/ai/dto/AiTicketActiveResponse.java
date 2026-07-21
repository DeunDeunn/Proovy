package com.deundeun.ai.dto;

import com.deundeun.ai.vo.AiTicketSubscriptionVo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AiTicketActiveResponse {

    private boolean hasActiveTicket;
    private Long subscriptionId;
    private Long planId;
    private String planName;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private String status;

    public static AiTicketActiveResponse active(AiTicketSubscriptionVo subscription) {
        return AiTicketActiveResponse.builder()
                .hasActiveTicket(true)
                .subscriptionId(subscription.getId())
                .planId(subscription.getPlanId())
                .planName(subscription.getPlanName())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .status(subscription.getStatus())
                .build();
    }

    public static AiTicketActiveResponse empty() {
        return AiTicketActiveResponse.builder()
                .hasActiveTicket(false)
                .build();
    }
}
