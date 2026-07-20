package com.deundeun.ai.dto;

import com.deundeun.ai.vo.AiTicketHistoryVo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AiTicketHistoryItemResponse {

    private Long id;
    private String type;
    private Long subscriptionId;
    private Long verificationPostId;
    private LocalDateTime createdAt;

    public static AiTicketHistoryItemResponse from(AiTicketHistoryVo history) {
        return AiTicketHistoryItemResponse.builder()
                .id(history.getId())
                .type(toResponseType(history.getType()))
                .subscriptionId(history.getSubscriptionId())
                .verificationPostId(null)
                .createdAt(history.getCreatedAt())
                .build();
    }

    private static String toResponseType(String type) {
        if ("PURCHASE".equals(type)) {
            return "PURCHASED";
        }
        if ("USE".equals(type)) {
            return "USED";
        }
        if ("EXPIRE".equals(type)) {
            return "EXPIRED";
        }
        if ("REFUND".equals(type)) {
            return "REFUNDED";
        }
        return type;
    }
}
