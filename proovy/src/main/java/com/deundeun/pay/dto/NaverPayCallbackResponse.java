package com.deundeun.pay.dto;

import com.deundeun.pay.enums.CashTransactionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NaverPayCallbackResponse {
    private Long chargeTransactionId;
    private CashTransactionStatus status;
}
