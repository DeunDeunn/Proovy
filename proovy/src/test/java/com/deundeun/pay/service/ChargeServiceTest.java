package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.client.NaverPayApiClient;
import com.deundeun.pay.config.NaverPayProperties;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.ChargeResponse;
import com.deundeun.pay.dto.NaverPayCallbackRequest;
import com.deundeun.pay.dto.NaverPayCallbackResponse;
import com.deundeun.pay.dto.naverpay.NaverPayApplyBody;
import com.deundeun.pay.dto.naverpay.NaverPayPaymentDetail;
import com.deundeun.pay.mapper.CashTransactionMapper;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ChargeServiceTest {

    @Mock
    private WalletService walletService;
    @Mock
    private CashTransactionMapper cashTransactionMapper;
    @Mock
    private NaverPayApiClient naverPayApiClient;
    @Mock
    private ChargeTransactionStateService chargeTransactionStateService;

    private ChargeService chargeService;

    @BeforeEach
    void setUp() {
        NaverPayProperties naverPayProperties = new NaverPayProperties(
                "dev-pay.paygate.naver.com", "clientId", "clientSecret", "chainId", "shopId",
                "http://localhost:3000/wallet/charge/return");
        chargeService = new ChargeService(
                walletService, cashTransactionMapper, naverPayProperties, naverPayApiClient,
                chargeTransactionStateService);
    }

    private NaverPayCallbackRequest callbackRequest() {
        NaverPayCallbackRequest request = new NaverPayCallbackRequest();
        ReflectionTestUtils.setField(request, "merchantPayKey", "CHG-7");
        ReflectionTestUtils.setField(request, "paymentId", "PAY123");
        return request;
    }

    @Test
    void handlePaymentCompleted_success_delegatesCompleteChargeToStateService() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.of(999L));
        when(cashTransactionMapper.selectById(7L)).thenReturn(transaction);

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));
        when(chargeTransactionStateService.completeCharge(transaction, detail, 999L)).thenReturn(true);

        NaverPayCallbackResponse response =
                chargeService.handlePaymentCompleted(callbackRequest());

        assertThat(response.getChargeTransactionId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(CashTransactionStatus.COMPLETED);
        verify(chargeTransactionStateService).completeCharge(transaction, detail, 999L);
    }

    @Test
    void handlePaymentCompleted_completeChargeRejectedByTokenMismatch_returnsActualCurrentStatus() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.of(999L));
        when(cashTransactionMapper.selectById(7L))
                .thenReturn(transaction)
                .thenReturn(CashTransaction.builder()
                        .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.FAILED)
                        .build());

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));
        // 보정 스케줄러가 먼저 lease를 가져가 FAILED로 확정한 뒤라 토큰이 안 맞아 거부됨
        when(chargeTransactionStateService.completeCharge(transaction, detail, 999L)).thenReturn(false);

        NaverPayCallbackResponse response =
                chargeService.handlePaymentCompleted(callbackRequest());

        assertThat(response.getStatus()).isEqualTo(CashTransactionStatus.FAILED);
    }

    @Test
    void handlePaymentCompleted_alreadyProcessedOrProcessing_doesNotReprocess() {
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.empty());
        CashTransaction existing = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.COMPLETED)
                .build();
        when(cashTransactionMapper.selectById(7L)).thenReturn(existing);

        NaverPayCallbackResponse response =
                chargeService.handlePaymentCompleted(callbackRequest());

        assertThat(response.getStatus()).isEqualTo(CashTransactionStatus.COMPLETED);
        verify(naverPayApiClient, never()).applyPayment(any());
        verify(chargeTransactionStateService, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void handlePaymentCompleted_transactionNotFound_throwsNotFound() {
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.empty());
        when(cashTransactionMapper.selectById(7L)).thenReturn(null);

        assertThatThrownBy(() -> chargeService.handlePaymentCompleted(callbackRequest()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHARGE_TRANSACTION_NOT_FOUND);
    }

    @Test
    void handlePaymentCompleted_merchantPayKeyMismatch_throwsAmountMismatch() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.of(999L));
        when(cashTransactionMapper.selectById(7L)).thenReturn(transaction);

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-999", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));
        when(chargeTransactionStateService.markFailed(7L, 999L)).thenReturn(true);

        assertThatThrownBy(() -> chargeService.handlePaymentCompleted(callbackRequest()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_AMOUNT_MISMATCH);

        verify(chargeTransactionStateService).markFailed(7L, 999L);
    }

    @Test
    void handlePaymentCompleted_markFailedRejectedByTokenMismatch_returnsActualCurrentStatusInsteadOfThrowing() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.of(999L));
        when(cashTransactionMapper.selectById(7L))
                .thenReturn(transaction)
                .thenReturn(CashTransaction.builder()
                        .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.COMPLETED)
                        .build());

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-999", "SUCCESS", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));
        // 보정 스케줄러가 먼저 lease를 가져가 COMPLETED로 확정한 뒤라 토큰이 안 맞아 거부됨
        when(chargeTransactionStateService.markFailed(7L, 999L)).thenReturn(false);

        NaverPayCallbackResponse response =
                chargeService.handlePaymentCompleted(callbackRequest());

        assertThat(response.getStatus()).isEqualTo(CashTransactionStatus.COMPLETED);
    }

    @Test
    void handlePaymentCompleted_amountMismatch_throwsAmountMismatch() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.of(999L));
        when(cashTransactionMapper.selectById(7L)).thenReturn(transaction);

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "SUCCESS", 5_000L, 5_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));
        when(chargeTransactionStateService.markFailed(7L, 999L)).thenReturn(true);

        assertThatThrownBy(() -> chargeService.handlePaymentCompleted(callbackRequest()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_AMOUNT_MISMATCH);

        verify(chargeTransactionStateService).markFailed(7L, 999L);
    }

    @Test
    void handlePaymentCompleted_notAdmitted_returnsFailedStatus() {
        CashTransaction transaction = CashTransaction.builder()
                .id(7L).walletId(6L).amount(10_000L).status(CashTransactionStatus.PROCESSING)
                .build();
        when(chargeTransactionStateService.beginProcessing(7L, "PAY123")).thenReturn(Optional.of(999L));
        when(cashTransactionMapper.selectById(7L)).thenReturn(transaction);

        NaverPayPaymentDetail detail = new NaverPayPaymentDetail(
                "PAY123", "hist1", "merchant1", "CHG-7", "FAIL", 10_000L, 10_000L, 0L, "프루비 캐시 충전");
        when(naverPayApiClient.applyPayment("PAY123")).thenReturn(new NaverPayApplyBody("PAY123", detail));
        when(chargeTransactionStateService.markFailed(7L, 999L)).thenReturn(true);

        NaverPayCallbackResponse response =
                chargeService.handlePaymentCompleted(callbackRequest());

        assertThat(response.getStatus()).isEqualTo(CashTransactionStatus.FAILED);
        verify(chargeTransactionStateService).markFailed(7L, 999L);
        verify(chargeTransactionStateService, never()).completeCharge(any(), any(), anyLong());
    }

    @Test
    void requestCharge_amountBelowMinimum_throwsInvalidChargeAmount() {
        assertThatThrownBy(() -> chargeService.requestCharge(1L, 500L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
    }

    @Test
    void requestCharge_amountAboveMaximum_throwsInvalidChargeAmount() {
        assertThatThrownBy(() -> chargeService.requestCharge(1L, 60_000L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
    }

    @Test
    void requestCharge_amountNotUnitOf1000_throwsInvalidChargeAmount() {
        assertThatThrownBy(() -> chargeService.requestCharge(1L, 1_500L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
    }

    @Test
    void requestCharge_validAmount_createsPendingTransactionAndReturnsMerchantPayKey() {
        Wallet wallet = Wallet.builder().id(6L).chargedBalance(5_000L).build();
        when(walletService.getOrCreateWallet(1L)).thenReturn(wallet);
        doAnswer(invocation -> {
            CashTransaction arg = invocation.getArgument(0);
            arg.setId(7L);
            return null;
        }).when(cashTransactionMapper).insert(any(CashTransaction.class));

        ChargeResponse response = chargeService.requestCharge(1L, 10_000L);

        assertThat(response.getMerchantPayKey()).isEqualTo("CHG-7");
        assertThat(response.getTotalPayAmount()).isEqualTo(10_000L);
        assertThat(response.getStatus()).isEqualTo(CashTransactionStatus.PENDING);
        verify(cashTransactionMapper).insert(any(CashTransaction.class));
    }
}
