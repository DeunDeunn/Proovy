package com.deundeun.pay.dto;

import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.domain.CashTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionItem {
    private Long id;
    private CashTransactionType type;
    private Long amount;
    private Long balanceAfter;
    private Long referenceId;
    private CashTransactionStatus status;
    private LocalDateTime createdAt;
}
