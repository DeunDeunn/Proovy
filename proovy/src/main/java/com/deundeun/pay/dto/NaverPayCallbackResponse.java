package com.deundeun.pay.dto;

import com.deundeun.pay.domain.CashTransactionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NaverPayCallbackResponse {
    private Long chargeTransactionId;
    private CashTransactionStatus status;
}
