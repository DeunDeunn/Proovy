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

/**
 * 충전 콜백 처리 중 PG 호출(naverPayApiClient.applyPayment) 전후로 트랜잭션을 짧게 나누기 위한 협력 클래스.
 * ChargeService 안에 두면 this.xxx() 자기 호출이 되어 @Transactional이 프록시를 안 거치고 무시되므로 별도 빈으로 분리했다.
 */
@Service
@RequiredArgsConstructor
public class ChargeTransactionStateService {

    private final CashTransactionMapper cashTransactionMapper;
    private final ChargeLotMapper chargeLotMapper;
    private final WalletService walletService;

    @Transactional
    public boolean beginProcessing(Long transactionId, String paymentId) {
        return cashTransactionMapper.beginProcessing(transactionId, paymentId) == 1;
    }

    /**
     * PROCESSING일 때만 FAILED로 전환한다 - 원래 요청과 보정 스케줄러가 같은 거래를
     * 동시에 처리하려다 한쪽이 completeCharge로 COMPLETED를 이미 확정한 뒤에, 뒤늦게
     * 도착한 쪽이 그 결과를 FAILED로 덮어써버리는 것을 막기 위함.
     */
    @Transactional
    public void markFailed(Long transactionId) {
        cashTransactionMapper.failFromProcessing(transactionId);
    }

    /**
     * 원래 콜백 요청과 보정 스케줄러가 같은 거래를 동시에 완료 처리하려 할 수 있어(원래
     * 요청이 30초 넘게 걸리면 스케줄러도 같은 걸 집어갈 수 있음), row를 잠그고 여전히
     * PROCESSING인지 다시 확인한 뒤에만 잔액 반영을 진행한다 - 아니면 이미 누가 끝낸 것.
     */
    @Transactional
    public void completeCharge(CashTransaction transaction, NaverPayPaymentDetail detail) {
        CashTransaction locked = cashTransactionMapper.selectByIdForUpdate(transaction.getId());
        if (locked.getStatus() != CashTransactionStatus.PROCESSING) {
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
}
