package com.deundeun.pay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettlementHistoryItem {
    private Long challengeId;
    private String title;
    // Lombok의 isSuccess() getter를 Jackson이 "is" 접두사를 벗겨 success로 직렬화하는 것을 막기 위해 키를 고정
    @JsonProperty("isSuccess")
    private boolean isSuccess;
    private LocalDateTime settledAt;
    private Long profitAmount;
}
