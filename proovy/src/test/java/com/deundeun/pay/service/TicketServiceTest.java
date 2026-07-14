package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.mapper.CashTransactionMapper;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private WalletService walletService;
    @Mock
    private CashTransactionMapper cashTransactionMapper;

    @InjectMocks
    private TicketService ticketService;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 100L;
    private static final Long REFERENCE_ID = 50L; // ai_ticket_subscriptions.id

    private Wallet walletWith(long charged, long lockedCharged) {
        return Wallet.builder().id(WALLET_ID).chargedBalance(charged).rewardBalance(0L)
                .lockedChargedBalance(lockedCharged).lockedRewardBalance(0L).build();
    }

    private void stubInsertGeneratesId(long generatedId) {
        doAnswer(invocation -> {
            CashTransaction arg = invocation.getArgument(0);
            arg.setId(generatedId);
            return null;
        }).when(cashTransactionMapper).insert(any(CashTransaction.class));
    }

    @Test
    void purchase_success_deductsChargedBalanceAndChargeLots() {
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(10_000L, 0L));
        stubInsertGeneratesId(1L);

        Long transactionId = ticketService.purchase(USER_ID, 7_000L, REFERENCE_ID);

        assertThat(transactionId).isEqualTo(1L);
        verify(walletService).updateChargedBalance(WALLET_ID, 3_000L);
        verify(walletService).deductChargeLotsFifo(WALLET_ID, 7_000L);

        verify(cashTransactionMapper).insert(argThat(t ->
                t.getType() == CashTransactionType.AI_TICKET_PURCHASE
                        && t.getAmount() == 7_000L
                        && t.getBalanceAfter() == 3_000L
                        && t.getReferenceId().equals(REFERENCE_ID)));
    }

    @Test
    void purchase_insufficientChargedBalance_throwsAndDoesNothing() {
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(1_000L, 0L));

        assertThatThrownBy(() -> ticketService.purchase(USER_ID, 7_000L, REFERENCE_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(walletService, never()).updateChargedBalance(anyLong(), anyLong());
        verify(walletService, never()).deductChargeLotsFifo(anyLong(), anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void purchase_alreadyPurchasedForSameReferenceId_throwsAndDoesNotDoubleDeduct() {
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(10_000L, 0L));
        when(cashTransactionMapper.selectByWalletIdAndReferenceIdAndType(
                WALLET_ID, REFERENCE_ID, CashTransactionType.AI_TICKET_PURCHASE))
                .thenReturn(CashTransaction.builder().id(1L).amount(7_000L).build());

        assertThatThrownBy(() -> ticketService.purchase(USER_ID, 7_000L, REFERENCE_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ALREADY_PURCHASED);

        verify(walletService, never()).updateChargedBalance(anyLong(), anyLong());
        verify(walletService, never()).deductChargeLotsFifo(anyLong(), anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void purchase_zeroAmount_throwsAndDoesNotTouchWalletOrHistory() {
        assertThatThrownBy(() -> ticketService.purchase(USER_ID, 0L, REFERENCE_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(walletService, never()).getWalletForUpdate(anyLong());
        verify(walletService, never()).updateChargedBalance(anyLong(), anyLong());
        verify(walletService, never()).deductChargeLotsFifo(anyLong(), anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void purchase_negativeAmount_throwsAndDoesNotTouchWalletOrHistory() {
        assertThatThrownBy(() -> ticketService.purchase(USER_ID, -7_000L, REFERENCE_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(walletService, never()).getWalletForUpdate(anyLong());
        verify(walletService, never()).updateChargedBalance(anyLong(), anyLong());
        verify(walletService, never()).deductChargeLotsFifo(anyLong(), anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void purchase_lockedByOtherChallenge_excludedFromUnlockedChargedBalance_throwsInsufficientBalance() {
        // charged_balance는 7,000 있지만 전부 다른 챌린지에 홀딩되어 있어 실제 구매 가능액은 0
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(7_000L, 7_000L));

        assertThatThrownBy(() -> ticketService.purchase(USER_ID, 1_000L, REFERENCE_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(walletService, never()).updateChargedBalance(anyLong(), anyLong());
        verify(walletService, never()).deductChargeLotsFifo(anyLong(), anyLong());
    }
}
