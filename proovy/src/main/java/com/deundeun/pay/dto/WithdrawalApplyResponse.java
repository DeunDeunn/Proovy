package com.deundeun.pay.dto;

import com.deundeun.pay.enums.SourceType;
import com.deundeun.pay.enums.WithdrawalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WithdrawalApplyResponse {
    private Long withdrawalRequestId;
    private SourceType sourceType;
    private Long amount;
    private Long feeAmount;
    private Long netTransferAmount;
    private WithdrawalStatus status;
    private LocalDateTime requestedAt;
}
