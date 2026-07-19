package com.deundeun.pay.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettlementHistoryItem {
    private Long challengeId;
    private String title;
    private boolean isSuccess;
    private LocalDateTime settledAt;
    private Long profitAmount;
}
