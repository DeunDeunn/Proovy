package com.deundeun.pay.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.client.NaverPayApiClient;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.dto.naverpay.NaverPayApplyBody;
import com.deundeun.pay.dto.naverpay.NaverPayPaymentDetail;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.mapper.CashTransactionMapper;

@ExtendWith(MockitoExtension.class)
class ChargeReconciliationSchedulerTest {

    private static final long ORIGINAL_TOKEN = 111L;
    private static final long LEASE_TOKEN = 222L;

    @Mock
    private CashTransactionMapper cashTransactionMapper;
    @Mock
    private NaverPayApiClient naverPayApiClient;
    @Mock
    private ChargeTransactionStateService chargeTransactionStateService;

    private ChargeReconciliationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ChargeReconciliationScheduler(
                cashTransactionMapper, naverPayApiClient, chargeTransactionStateService);
    }

    private CashTransaction stuckTransaction() {
        return CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L)
                .status(CashTransactionStatus.PROCESSING)
                .pgTransactionId("PAY123")
                .processingToken(ORIGINAL_TOKEN)
                .build();
    }

    private void stubLeaseGranted() {
        when(chargeTransactionStateService.claimForReconciliation(7L, ORIGINAL_TOKEN))
                .thenReturn(Optional.of(LEASE_TOKEN));
    }

    @Test
    void reconcileStuckCharges_noStuckTransactions_doesNothing() {
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of());

        scheduler.reconcileStuckCharges();

        verify(naverPayApiClient, never()).applyPayment(any());
    }

    @Test
    void reconcileStuckCharges_originalCallbackStillFinishing_leaseNotGranted_skipsWithoutCallingPg() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        when(chargeTransactionStateService.claimForReconciliation(7L, ORIGINAL_TOKEN))
                .thenReturn(Optional.empty());

        scheduler.reconcileStuckCharges();

        verify(naverPayApiClient, never()).applyPayment(any());
        verify(chargeTransactionStateService, never()).markFailed(anyLong(), anyLong());
        verify(chargeTransactionStateService, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void reconcileStuckCharges_paymentActuallySucceeded_completesCharge() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        stubLeaseGranted();

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService).completeCharge(transaction, detail, LEASE_TOKEN);
        verify(chargeTransactionStateService, never()).markFailed(anyLong(), anyLong());
    }

    @Test
    void reconcileStuckCharges_merchantPayKeyMismatch_marksFailed() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        stubLeaseGranted();

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-999", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService).markFailed(7L, LEASE_TOKEN);
        verify(chargeTransactionStateService, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void reconcileStuckCharges_notAdmitted_marksFailed() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        stubLeaseGranted();

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "FAIL", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService).markFailed(7L, LEASE_TOKEN);
    }

    @Test
    void reconcileStuckCharges_amountMismatch_marksFailed() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        stubLeaseGranted();

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 5_000L, 5_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService).markFailed(7L, LEASE_TOKEN);
    }

    @Test
    void reconcileStuckCharges_applyPaymentThrows_doesNotPropagateAndLeavesTransactionUntouched() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        stubLeaseGranted();
        when(naverPayApiClient.applyPayment("PAY123")).thenThrow(new RuntimeException("PG 통신 실패"));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService, never()).markFailed(anyLong(), anyLong());
        verify(chargeTransactionStateService, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void reconcileStuckCharges_pgTimeExpired_marksFailedInsteadOfRetrying() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        stubLeaseGranted();
        when(naverPayApiClient.applyPayment("PAY123"))
                .thenThrow(new ApiException(ErrorCode.PG_TIME_EXPIRED));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService).markFailed(7L, LEASE_TOKEN);
        verify(chargeTransactionStateService, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void reconcileStuckCharges_pgServiceUnavailable_doesNotMarkFailed_retriesNextCycle() {
        CashTransaction transaction = stuckTransaction();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction));
        stubLeaseGranted();
        when(naverPayApiClient.applyPayment("PAY123"))
                .thenThrow(new ApiException(ErrorCode.PG_SERVICE_UNAVAILABLE));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService, never()).markFailed(anyLong(), anyLong());
        verify(chargeTransactionStateService, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void reconcileStuckCharges_multipleStuckTransactions_processesEachIndependently() {
        CashTransaction transaction1 = stuckTransaction();
        CashTransaction transaction2 = CashTransaction.builder()
                .id(8L).walletId(9L).amount(20_000L)
                .status(CashTransactionStatus.PROCESSING)
                .pgTransactionId("PAY456")
                .processingToken(333L)
                .build();
        when(cashTransactionMapper.selectStuckProcessing(any())).thenReturn(List.of(transaction1, transaction2));
        stubLeaseGranted();
        when(chargeTransactionStateService.claimForReconciliation(8L, 333L))
                .thenReturn(Optional.of(444L));

        when(naverPayApiClient.applyPayment("PAY123")).thenThrow(new RuntimeException("PG 통신 실패"));
        NaverPayPaymentDetail detail2 = new NaverPayPaymentDetail(
                "PAY456", "hist2", "merchant2", "CHG-8", "SUCCESS", 20_000L, 20_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY456")).thenReturn(new NaverPayApplyBody("PAY456", detail2));

        scheduler.reconcileStuckCharges();

        verify(chargeTransactionStateService, never()).markFailed(7L, LEASE_TOKEN);
        verify(chargeTransactionStateService).completeCharge(transaction2, detail2, 444L);
    }
}
