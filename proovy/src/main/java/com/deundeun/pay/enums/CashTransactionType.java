package com.deundeun.pay.enums;

public enum CashTransactionType {
    CHARGE,
    CHALLENGE_HOLD,
    CHALLENGE_PRINCIPAL_REFUND, // 챌린지 시작 전 취소 시 홀딩 해제용 (정산과는 별개, 아직 미구현)
    CHALLENGE_PRINCIPAL_SUCCESS,
    CHALLENGE_PRINCIPAL_FAIL,
    CHALLENGE_PROFIT_DISTRIBUTION,
    HOST_FEE,
    WITHDRAWAL,
    WITHDRAWAL_REFUND,
    AI_TICKET_PURCHASE,
    AI_TICKET_REFUND
}
