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

    @Transactional
    public void markFailed(Long transactionId) {
        cashTransactionMapper.updateStatus(transactionId, CashTransactionStatus.FAILED);
    }

    @Transactional
    public void completeCharge(CashTransaction transaction, NaverPayPaymentDetail detail) {
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
