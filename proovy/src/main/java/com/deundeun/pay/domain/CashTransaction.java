package com.deundeun.pay.domain;

import com.deundeun.pay.enums.CashTransactionStatus;
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
public class CashTransaction {
    private Long id;
    private Long walletId;
    private CashTransactionType type;
    private Long amount;
    private Long balanceAfter;
    private String pgTransactionId;
    private CashTransactionStatus status;
    private Long referenceId;
    private LocalDateTime createdAt;
}
