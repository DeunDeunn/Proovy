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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.domain.WithdrawalRequest;
import com.deundeun.pay.dto.WithdrawalApplyRequest;
import com.deundeun.pay.dto.WithdrawalApplyResponse;
import com.deundeun.pay.dto.WithdrawalHistoryResponse;
import com.deundeun.pay.dto.WithdrawalItem;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.enums.SourceType;
import com.deundeun.pay.enums.WithdrawalStatus;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.WithdrawalMapper;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private WithdrawalMapper withdrawalMapper;
    @Mock
    private WalletService walletService;
    @Mock
    private CashTransactionMapper cashTransactionMapper;

    @InjectMocks
    private WithdrawalService withdrawalService;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 100L;

    private Wallet walletWith(long charged, long reward, long locked) {
        return Wallet.builder().id(WALLET_ID).chargedBalance(charged).rewardBalance(reward).lockedBalance(locked).build();
    }

    private WithdrawalApplyRequest requestOf(SourceType sourceType, long amount) {
        return WithdrawalApplyRequest.builder()
                .sourceType(sourceType)
                .amount(amount)
                .bankName("국민은행")
                .accountNumber("110123456789")
                .accountHolderName("홍길동")
                .build();
    }

    private void stubInsertGeneratesId(long generatedId) {
        doAnswer(invocation -> {
            WithdrawalRequest arg = invocation.getArgument(0);
            arg.setId(generatedId);
            return null;
        }).when(withdrawalMapper).insert(any(WithdrawalRequest.class));
    }

    @Test
    void applyWithdrawal_reward_success() {
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(0L, 10_000L, 0L));
        stubInsertGeneratesId(1L);

        WithdrawalApplyResponse response = withdrawalService.applyWithdrawal(USER_ID, requestOf(SourceType.REWARD, 10_000L));

        // 리워드 수수료 1% -> 100원, 실지급 9,900원
        assertThat(response.getWithdrawalRequestId()).isEqualTo(1L);
        assertThat(response.getFeeAmount()).isEqualTo(100L);
        assertThat(response.getNetTransferAmount()).isEqualTo(9_900L);
        assertThat(response.getStatus()).isEqualTo(WithdrawalStatus.PENDING);

        verify(walletService).updateRewardBalance(WALLET_ID, 0L);
        verify(walletService, never()).updateChargedBalance(anyLong(), anyLong());
        verify(walletService, never()).deductChargeLotsFifo(anyLong(), anyLong());

        verify(cashTransactionMapper).insert(argThat(t ->
                t.getType() == CashTransactionType.WITHDRAWAL && t.getAmount() == 10_000L));
    }

    @Test
    void applyWithdrawal_reward_belowMinimum_throwsInvalidWithdrawalAmount() {
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(0L, 10_000L, 0L));

        assertThatThrownBy(() -> withdrawalService.applyWithdrawal(USER_ID, requestOf(SourceType.REWARD, 4_999L)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_WITHDRAWAL_AMOUNT);

        verify(walletService, never()).updateRewardBalance(anyLong(), anyLong());
        verify(withdrawalMapper, never()).insert(any());
    }

    @Test
    void applyWithdrawal_reward_insufficientBalance_throws() {
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(0L, 5_000L, 0L));

        assertThatThrownBy(() -> withdrawalService.applyWithdrawal(USER_ID, requestOf(SourceType.REWARD, 10_000L)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(walletService, never()).updateRewardBalance(anyLong(), anyLong());
        verify(withdrawalMapper, never()).insert(any());
    }

    @Test
    void applyWithdrawal_charged_success() {
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(20_000L, 0L, 0L));
        when(walletService.getChargedWithdrawableAmount(WALLET_ID)).thenReturn(20_000L);
        stubInsertGeneratesId(2L);

        WithdrawalApplyResponse response = withdrawalService.applyWithdrawal(USER_ID, requestOf(SourceType.CHARGED, 20_000L));

        // 충전 수수료 5% -> 1,000원, 실지급 19,000원
        assertThat(response.getFeeAmount()).isEqualTo(1_000L);
        assertThat(response.getNetTransferAmount()).isEqualTo(19_000L);

        verify(walletService).updateChargedBalance(WALLET_ID, 0L);
        verify(walletService).deductChargeLotsFifo(WALLET_ID, 20_000L);
        verify(walletService, never()).updateRewardBalance(anyLong(), anyLong());
    }

    @Test
    void applyWithdrawal_charged_belowSevenDayWithdrawable_throwsInsufficientBalance() {
        // charged_balance 총액은 충분해도(20,000), 7일 안 지난 lot이 대부분이라 출금 가능액은 3,000뿐인 상황
        when(walletService.getWalletForUpdate(USER_ID)).thenReturn(walletWith(20_000L, 0L, 0L));
        when(walletService.getChargedWithdrawableAmount(WALLET_ID)).thenReturn(3_000L);

        assertThatThrownBy(() -> withdrawalService.applyWithdrawal(USER_ID, requestOf(SourceType.CHARGED, 10_000L)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(walletService, never()).updateChargedBalance(anyLong(), anyLong());
        verify(walletService, never()).deductChargeLotsFifo(anyLong(), anyLong());
        verify(withdrawalMapper, never()).insert(any());
    }

    @Test
    void getWithdrawalHistory_returnsPageWithCorrectTotalPages() {
        when(walletService.getOrCreateWallet(USER_ID)).thenReturn(walletWith(0L, 0L, 0L));
        List<WithdrawalItem> content = List.of(
                WithdrawalItem.builder().id(2L).amount(10_000L).status(WithdrawalStatus.PENDING).build(),
                WithdrawalItem.builder().id(1L).amount(5_000L).status(WithdrawalStatus.COMPLETED).build());
        when(withdrawalMapper.selectByWalletId(WALLET_ID, null, 0, 10)).thenReturn(content);
        when(withdrawalMapper.countByWalletId(WALLET_ID, null)).thenReturn(12L);

        WithdrawalHistoryResponse response = withdrawalService.getWithdrawalHistory(USER_ID, null, 0, 10);

        assertThat(response.getContent()).isEqualTo(content);
        assertThat(response.getTotalElements()).isEqualTo(12L);
        assertThat(response.getTotalPages()).isEqualTo(2); // ceil(12/10)
    }
}
