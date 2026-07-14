package com.deundeun.pay.service;

/**
 * AI 티켓 도메인이 티켓 구매 시 유저 지갑에서 금액을 정산할 때 쓰는 계약.
 * 지갑 모듈은 티켓 상품/구독 정보를 알 필요가 없고, referenceId는 호출하는 쪽 도메인의
 * 식별자(ai_ticket_subscriptions.id)를 그대로 cash_transactions.reference_id에 남기는 용도다.
 * 환불은 지원하지 않는다 (도메인 정책상 티켓 구매는 환불 대상이 아님).
 */
public interface WalletTicketService {

    /**
     * userId의 지갑에서 amount만큼 티켓 구매 대금을 charged_balance에서 영구히 차감한다.
     * reward_balance는 건드리지 않는다(충전 캐시로만 구매 가능). 플랫폼 내부에서 바로
     * 소비되는 지출이라 출금 전용 7일 대기 규칙과는 무관하게, 다른 챌린지에 홀딩되지 않은
     * charged_balance 전체를 기준으로 검증한다.
     *
     * @param userId      구매자 userId
     * @param amount      구매 금액 (1원 이상)
     * @param referenceId 이 구매의 근거가 되는 ai_ticket_subscriptions.id
     * @return 이번 구매 건의 거래 id (cash_transactions.id)
     * @throws com.deundeun.global.exception.ApiException INSUFFICIENT_BALANCE — 홀딩되지 않은 충전 잔액이 amount보다 적을 때
     */
    Long purchase(Long userId, long amount, Long referenceId);
}
