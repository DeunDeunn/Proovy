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
    // 필드에 @JsonProperty를 붙이면 Jackson이 getter(isSuccess())에서 유추한 "success"와
    // 별개 프로퍼티로 취급해 success/isSuccess가 중복으로 직렬화된다. 생성되는 getter
    // 메서드 자체에 annotation을 붙여야 "isSuccess" 하나로만 나간다.
    @Getter(onMethod_ = @__(@JsonProperty("isSuccess")))
    private boolean isSuccess;
    private LocalDateTime settledAt;
    private Long profitAmount;
}
