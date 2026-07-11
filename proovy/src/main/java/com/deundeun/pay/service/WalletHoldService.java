package com.deundeun.pay.service;

/**
 * 다른 도메인(예: Room/챌린지)이 유저의 지갑에서 참가비 등을 홀딩할 때 쓰는 계약.
 * 지갑 모듈은 이 홀딩이 "왜" 필요한지(챌린지 참가 등) 알 필요가 없고,
 * referenceId는 호출하는 쪽 도메인의 식별자를 그대로 cash_transactions.reference_id에 남기는 용도다.
 */
public interface WalletHoldService {

    /**
     * userId의 지갑에서 amount만큼 홀딩한다. charged_balance/reward_balance는 그대로 두고
     * locked_balance만 늘려서, 사용 가능 잔액(availableBalance)에서 그만큼을 뺀다.
     *
     * @param userId      홀딩 대상 유저 id
     * @param amount      홀딩할 금액 (1원 이상)
     * @param referenceId 홀딩 사유가 되는 대상 id (예: 챌린지/방 id). cash_transactions.reference_id에 기록됨
     * @return 이번 홀딩 건의 거래 id (cash_transactions.id) — 나중에 홀딩 해제 시 참조용
     * @throws com.deundeun.global.exception.ApiException INSUFFICIENT_BALANCE — 사용 가능 잔액이 amount보다 적을 때
     */
    Long hold(Long userId, long amount, Long referenceId);
}
