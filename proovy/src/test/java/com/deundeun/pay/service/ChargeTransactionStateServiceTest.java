package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.naverpay.NaverPayPaymentDetail;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.ChargeLotMapper;

@ExtendWith(MockitoExtension.class)
class ChargeTransactionStateServiceTest {

    @Mock
    private CashTransactionMapper cashTransactionMapper;
    @Mock
    private ChargeLotMapper chargeLotMapper;
    @Mock
    private WalletService walletService;

    private ChargeTransactionStateService chargeTransactionStateService;

    @BeforeEach
    void setUp() {
        chargeTransactionStateService =
                new ChargeTransactionStateService(cashTransactionMapper, chargeLotMapper, walletService);
    }

    @Test
    void beginProcessing_oneRowAffected_returnsTrue() {
        when(cashTransactionMapper.beginProcessing(7L, "PAY123")).thenReturn(1);

        boolean claimed = chargeTransactionStateService.beginProcessing(7L, "PAY123");

        assertThat(claimed).isTrue();
    }

    @Test
    void beginProcessing_noRowAffected_returnsFalse() {
        when(cashTransactionMapper.beginProcessing(7L, "PAY123")).thenReturn(0);

        boolean claimed = chargeTransactionStateService.beginProcessing(7L, "PAY123");

        assertThat(claimed).isFalse();
    }

    @Test
    void markFailed_updatesStatusToFailed() {
        chargeTransactionStateService.markFailed(7L);

        verify(cashTransactionMapper).failFromProcessing(7L);
    }

    @Test
    void completeCharge_stillProcessing_creditsWalletInsertsChargeLotAndCompletesTransaction() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");

        when(cashTransactionMapper.selectByIdForUpdate(7L)).thenReturn(transaction);
        Wallet wallet = Wallet.builder().id(6L).chargedBalance(5_000L).build();
        when(walletService.getWalletByIdForUpdate(6L)).thenReturn(wallet);

        chargeTransactionStateService.completeCharge(transaction, detail);

        verify(walletService).updateChargedBalance(6L, 15_000L);
        verify(chargeLotMapper).insert(any(ChargeLot.class));
        verify(cashTransactionMapper).completeCharge(7L, "PAY123", 15_000L);
    }

    @Test
    void completeCharge_alreadyCompletedByConcurrentCaller_doesNotDoubleApply() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        CashTransaction alreadyCompleted = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.COMPLETED)
                .build();
        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");

        when(cashTransactionMapper.selectByIdForUpdate(7L)).thenReturn(alreadyCompleted);

        chargeTransactionStateService.completeCharge(transaction, detail);

        verify(walletService, never()).updateChargedBalance(any(), anyLong());
        verify(chargeLotMapper, never()).insert(any());
        verify(cashTransactionMapper, never()).completeCharge(any(), any(), anyLong());
    }
}
