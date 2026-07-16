package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
    void beginProcessing_oneRowAffected_returnsToken() {
        when(cashTransactionMapper.beginProcessing(anyLong(), any(), anyLong())).thenReturn(1);

        Optional<Long> claim = chargeTransactionStateService.beginProcessing(7L, "PAY123");

        assertThat(claim).isPresent();
    }

    @Test
    void beginProcessing_noRowAffected_returnsEmpty() {
        when(cashTransactionMapper.beginProcessing(anyLong(), any(), anyLong())).thenReturn(0);

        Optional<Long> claim = chargeTransactionStateService.beginProcessing(7L, "PAY123");

        assertThat(claim).isEmpty();
    }

    @Test
    void markFailed_tokenMatches_updatesStatusToFailedAndReturnsTrue() {
        when(cashTransactionMapper.failFromProcessing(7L, 999L)).thenReturn(1);

        boolean applied = chargeTransactionStateService.markFailed(7L, 999L);

        assertThat(applied).isTrue();
        verify(cashTransactionMapper).failFromProcessing(7L, 999L);
    }

    @Test
    void markFailed_tokenMismatch_returnsFalse() {
        when(cashTransactionMapper.failFromProcessing(7L, 999L)).thenReturn(0);

        boolean applied = chargeTransactionStateService.markFailed(7L, 999L);

        assertThat(applied).isFalse();
    }

    @Test
    void completeCharge_stillProcessingWithMatchingToken_creditsWalletInsertsChargeLotAndCompletesTransaction() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .processingToken(999L)
                .build();
        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");

        when(cashTransactionMapper.selectByIdForUpdate(7L)).thenReturn(transaction);
        Wallet wallet = Wallet.builder().id(6L).chargedBalance(5_000L).build();
        when(walletService.getWalletByIdForUpdate(6L)).thenReturn(wallet);

        boolean applied = chargeTransactionStateService.completeCharge(transaction, detail, 999L);

        assertThat(applied).isTrue();
        verify(walletService).updateChargedBalance(6L, 15_000L);
        verify(chargeLotMapper).insert(any(ChargeLot.class));
        verify(cashTransactionMapper).completeCharge(7L, "PAY123", 15_000L);
    }

    @Test
    void completeCharge_alreadyCompletedByConcurrentCaller_doesNotDoubleApply() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .processingToken(999L)
                .build();
        CashTransaction alreadyCompleted = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.COMPLETED)
                .processingToken(999L)
                .build();
        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");

        when(cashTransactionMapper.selectByIdForUpdate(7L)).thenReturn(alreadyCompleted);

        boolean applied = chargeTransactionStateService.completeCharge(transaction, detail, 999L);

        assertThat(applied).isFalse();
        verify(walletService, never()).updateChargedBalance(any(), anyLong());
        verify(chargeLotMapper, never()).insert(any());
        verify(cashTransactionMapper, never()).completeCharge(any(), any(), anyLong());
    }

    /**
     * 원래 콜백이 PG 승인(60초까지 걸릴 수 있음)을 기다리는 중에, 보정 스케줄러가 30초
     * 임계값을 넘겨 먼저 claimForReconciliation으로 lease를 가져가는(processing_token이
     * 새 값으로 바뀌는) 경쟁 순서를 재현한다. 그 뒤 원래 콜백이 자신의 옛 토큰으로
     * completeCharge를 시도해도 반영되지 않아야 한다 - 그렇지 않으면 스케줄러가 이미
     * 내린 결정(완료/실패)을 원래 요청이 뒤늦게 덮어써버릴 수 있다.
     */
    @Test
    void completeCharge_reconciliationClaimedNewerTokenFirst_originalCallbackDoesNotApply() {
        long originalToken = 111L;
        long reconciliationToken = 222L;

        CashTransaction original = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .processingToken(originalToken)
                .build();
        CashTransaction takenOverByScheduler = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .processingToken(reconciliationToken)
                .build();
        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");

        when(cashTransactionMapper.selectByIdForUpdate(7L)).thenReturn(takenOverByScheduler);

        boolean applied = chargeTransactionStateService.completeCharge(original, detail, originalToken);

        assertThat(applied).isFalse();
        verify(walletService, never()).updateChargedBalance(any(), anyLong());
        verify(chargeLotMapper, never()).insert(any());
        verify(cashTransactionMapper, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void claimForReconciliation_matchingToken_returnsNewToken() {
        when(cashTransactionMapper.claimForReconciliation(anyLong(), anyLong(), anyLong())).thenReturn(1);

        Optional<Long> lease = chargeTransactionStateService.claimForReconciliation(7L, 111L);

        assertThat(lease).isPresent();
    }

    @Test
    void claimForReconciliation_tokenAlreadyChanged_returnsEmpty() {
        when(cashTransactionMapper.claimForReconciliation(anyLong(), anyLong(), anyLong())).thenReturn(0);

        Optional<Long> lease = chargeTransactionStateService.claimForReconciliation(7L, 111L);

        assertThat(lease).isEmpty();
    }
}
