package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.domain.CashTransactionType;
import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.ChargeLotMapper;
import com.deundeun.pay.mapper.WalletMapper;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private CashTransactionMapper cashTransactionMapper;

    @Mock
    private ChargeLotMapper chargeLotMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void getWalletView_subtractsLockedBalanceFromAvailableBalance() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder()
                .id(10L)
                .userId(userId)
                .chargedBalance(10_000L)
                .rewardBalance(2_000L)
                .lockedBalance(3_000L)
                .build();
        when(walletMapper.selectByUserId(userId)).thenReturn(wallet);

        WalletResponse response = walletService.getWalletView(userId);

        assertThat(response.getAvailableBalance()).isEqualTo(9_000L);
    }

    @Test
    void getOrCreateWallet_insertsIfAbsentThenSelectsByUserId() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder().id(10L).userId(userId).build();
        when(walletMapper.selectByUserId(userId)).thenReturn(wallet);

        Wallet result = walletService.getOrCreateWallet(userId);

        verify(walletMapper).insertIfAbsent(userId);
        assertThat(result).isEqualTo(wallet);
    }

    @Test
    void getTransactionHistory_computesTotalPagesAndMapsContent() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder().id(10L).userId(userId).build();
        when(walletMapper.selectByUserId(userId)).thenReturn(wallet);

        CashTransaction transaction = CashTransaction.builder()
                .id(5L)
                .type(CashTransactionType.CHARGE)
                .amount(10_000L)
                .balanceAfter(10_000L)
                .status(CashTransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.of(2026, 7, 11, 12, 0))
                .build();
        when(cashTransactionMapper.selectByWalletId(10L, null, 0, 10))
                .thenReturn(List.of(transaction));
        when(cashTransactionMapper.countByWalletId(10L, null)).thenReturn(23L);

        TransactionHistoryResponse response = walletService.getTransactionHistory(userId, null, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId()).isEqualTo(5L);
        assertThat(response.getTotalElements()).isEqualTo(23L);
        assertThat(response.getTotalPages()).isEqualTo(3); // ceil(23/10)
    }

    @Test
    void hold_success_updatesLockedBalanceAndDeductsSingleLotAndRecordsTransaction() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(10_000L).rewardBalance(0L).lockedBalance(0L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        ChargeLot lot = ChargeLot.builder().id(100L).walletId(10L).remainingAmount(10_000L).build();
        when(chargeLotMapper.selectRemainingByWalletIdOrderByChargedAtAsc(10L)).thenReturn(List.of(lot));

        doAnswer(invocation -> {
            CashTransaction arg = invocation.getArgument(0);
            arg.setId(555L);
            return null;
        }).when(cashTransactionMapper).insert(any(CashTransaction.class));

        Long transactionId = walletService.hold(userId, 3_000L, 99L);

        assertThat(transactionId).isEqualTo(555L);
        verify(walletMapper).updateLockedBalance(10L, 3_000L);
        verify(chargeLotMapper).updateRemainingAmount(100L, 7_000L);

        ArgumentCaptor<CashTransaction> captor = ArgumentCaptor.forClass(CashTransaction.class);
        verify(cashTransactionMapper).insert(captor.capture());
        CashTransaction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(CashTransactionType.CHALLENGE_HOLD);
        assertThat(saved.getAmount()).isEqualTo(3_000L);
        assertThat(saved.getReferenceId()).isEqualTo(99L);
        assertThat(saved.getStatus()).isEqualTo(CashTransactionStatus.COMPLETED);
        assertThat(saved.getBalanceAfter()).isEqualTo(7_000L); // availableBalance(10000) - 3000
    }

    @Test
    void hold_insufficientBalance_throwsAndDoesNotMutateAnything() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(1_000L).rewardBalance(0L).lockedBalance(0L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        assertThatThrownBy(() -> walletService.hold(userId, 5_000L, 99L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(walletMapper, never()).updateLockedBalance(anyLong(), anyLong());
        verify(chargeLotMapper, never()).selectRemainingByWalletIdOrderByChargedAtAsc(anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void hold_deductsAcrossMultipleLotsInFifoOrder() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(10_000L).rewardBalance(0L).lockedBalance(0L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        ChargeLot olderLot = ChargeLot.builder().id(100L).walletId(10L).remainingAmount(1_000L).build();
        ChargeLot newerLot = ChargeLot.builder().id(200L).walletId(10L).remainingAmount(5_000L).build();
        when(chargeLotMapper.selectRemainingByWalletIdOrderByChargedAtAsc(10L))
                .thenReturn(List.of(olderLot, newerLot)); // 오래된 순으로 이미 정렬돼서 온다고 가정

        walletService.hold(userId, 1_200L, 99L);

        InOrder inOrder = Mockito.inOrder(chargeLotMapper);
        inOrder.verify(chargeLotMapper).updateRemainingAmount(100L, 0L);      // 오래된 lot부터 다 소진
        inOrder.verify(chargeLotMapper).updateRemainingAmount(200L, 4_800L); // 남은 200만 다음 lot에서 차감
    }

    @Test
    void hold_amountExceedsChargeLots_stopsAtLastLotWithoutFailing() {
        Long userId = 1L;
        // charged 2,000 + reward 8,000 = availableBalance 10,000 (5,000 홀딩엔 충분)
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(2_000L).rewardBalance(8_000L).lockedBalance(0L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        ChargeLot lot = ChargeLot.builder().id(100L).walletId(10L).remainingAmount(2_000L).build();
        when(chargeLotMapper.selectRemainingByWalletIdOrderByChargedAtAsc(10L)).thenReturn(List.of(lot));

        walletService.hold(userId, 5_000L, 99L);

        // lot은 가진 만큼(2,000)만 차감되고, 나머지 3,000은 reward_balance 몫이라 lot 차감 없이 넘어감
        verify(chargeLotMapper).updateRemainingAmount(100L, 0L);
        verify(walletMapper).updateLockedBalance(10L, 5_000L);
    }
}
