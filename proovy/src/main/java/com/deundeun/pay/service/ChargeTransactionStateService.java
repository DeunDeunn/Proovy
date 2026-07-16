package com.deundeun.pay.service;

import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.naverpay.NaverPayPaymentDetail;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.ChargeLotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 충전 콜백 처리 중 PG 호출(naverPayApiClient.applyPayment) 전후로 트랜잭션을 짧게 나누기 위한 협력 클래스.
 * ChargeService 안에 두면 this.xxx() 자기 호출이 되어 @Transactional이 프록시를 안 거치고 무시되므로 별도 빈으로 분리했다.
 *
 * applyPayment의 readTimeout(60초)이 보정 스케줄러의 정지 판단 기준(30초)보다 길어서, 원래 콜백이
 * 아직 정상적으로 PG 응답을 기다리는 중에도 스케줄러가 같은 거래를 가져갈 수 있다. 그래서 PROCESSING
 * 진입 시 발급하는 processing_token을 일종의 lease로 써서, 그 이후의 모든 상태 전환(markFailed,
 * completeCharge, 보정 스케줄러의 재인수)이 자신이 쥔 토큰이 여전히 유효한지 확인하고서만 적용되게 한다.
 */
@Service
@RequiredArgsConstructor
public class ChargeTransactionStateService {

    private final CashTransactionMapper cashTransactionMapper;
    private final ChargeLotMapper chargeLotMapper;
    private final WalletService walletService;

    @Transactional
    public Optional<Long> beginProcessing(Long transactionId, String paymentId) {
        long token = ThreadLocalRandom.current().nextLong();
        int updated = cashTransactionMapper.beginProcessing(transactionId, paymentId, token);
        return updated == 1 ? Optional.of(token) : Optional.empty();
    }

    /**
     * 자신이 쥔 processingToken이 여전히 유효할 때만(즉 그 사이 보정 스케줄러가 가져가지 않았을 때만)
     * PROCESSING을 FAILED로 전환한다 - 그렇지 않으면 이미 다른 쪽이 처리 중이거나 끝낸 것이므로 무시.
     */
    @Transactional
    public void markFailed(Long transactionId, long processingToken) {
        cashTransactionMapper.failFromProcessing(transactionId, processingToken);
    }

    /**
     * row를 잠그고, 여전히 PROCESSING이면서 자신이 쥔 processingToken이 그대로인지 재확인한 뒤에만
     * 잔액 반영을 진행한다 - 아니면 이미 보정 스케줄러가 가져갔거나 다른 쪽이 끝낸 것.
     */
    @Transactional
    public void completeCharge(CashTransaction transaction, NaverPayPaymentDetail detail, long processingToken) {
        CashTransaction locked = cashTransactionMapper.selectByIdForUpdate(transaction.getId());
        if (locked.getStatus() != CashTransactionStatus.PROCESSING
                || locked.getProcessingToken() == null
                || locked.getProcessingToken() != processingToken) {
            return;
        }

        Wallet wallet = walletService.getWalletByIdForUpdate(transaction.getWalletId());
        long newChargedBalance = wallet.getChargedBalance() + transaction.getAmount();
        walletService.updateChargedBalance(wallet.getId(), newChargedBalance);

        ChargeLot chargeLot = ChargeLot.builder()
                .walletId(wallet.getId())
                .amount(transaction.getAmount())
                .remainingAmount(transaction.getAmount())
                .build();
        chargeLotMapper.insert(chargeLot);

        cashTransactionMapper.completeCharge(transaction.getId(), detail.paymentId(), newChargedBalance);
    }

    /**
     * 보정 스케줄러 전용 - 원래 콜백이 아직 살아있을 수도 있는 stuck PROCESSING 거래를 넘겨받으려 할 때,
     * 자신이 읽었던 processingToken이 그대로일 때만 새 토큰으로 교체해 소유권을 가져온다(lease 인수).
     * 실패하면(0건) 원래 요청이 그 사이 이미 끝낸 것이므로 보정을 시도하지 않는다.
     */
    @Transactional
    public Optional<Long> claimForReconciliation(Long transactionId, long expectedToken) {
        long newToken = ThreadLocalRandom.current().nextLong();
        int updated = cashTransactionMapper.claimForReconciliation(transactionId, expectedToken, newToken);
        return updated == 1 ? Optional.of(newToken) : Optional.empty();
    }
}
