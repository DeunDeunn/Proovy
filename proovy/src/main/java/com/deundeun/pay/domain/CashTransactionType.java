package com.deundeun.pay.domain;

public enum CashTransactionType {
    CHARGE,
    CHALLENGE_HOLD,
    CHALLENGE_PRINCIPAL_REFUND,
    CHALLENGE_PROFIT_DISTRIBUTION,
    HOST_FEE,
    WITHDRAWAL,
    AI_TICKET_PURCHASE,
    AI_TICKET_REFUND
}
