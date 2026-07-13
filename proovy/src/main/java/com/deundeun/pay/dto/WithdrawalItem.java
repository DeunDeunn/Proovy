package com.deundeun.pay.dto;

import com.deundeun.pay.enums.SourceType;
import com.deundeun.pay.enums.WithdrawalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WithdrawalItem {
    private Long id;
    private SourceType sourceType;
    private Long amount;
    private Long feeAmount;
    private Long netTransferAmount;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private WithdrawalStatus status;
    private String rejectReason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}
