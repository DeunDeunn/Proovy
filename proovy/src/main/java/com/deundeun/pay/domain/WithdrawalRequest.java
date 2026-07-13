package com.deundeun.pay.domain;

import com.deundeun.pay.enums.SourceType;
import com.deundeun.pay.enums.WithdrawalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest {
    private Long id;
    private Long walletId;
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
