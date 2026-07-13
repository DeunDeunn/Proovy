package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import com.deundeun.pay.domain.ChargeLotAllocation;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.ChargeLotAllocationMapper;
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

    @Mock
    private ChargeLotAllocationMapper chargeLotAllocationMapper;

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
                .lockedChargedBalance(3_000L)
                .lockedRewardBalance(0L)
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
    void hold_success_updatesLockedChargedBalanceAndDeductsSingleLotAndRecordsTransaction() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(10_000L).rewardBalance(0L)
                .lockedChargedBalance(0L).lockedRewardBalance(0L)
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
        // 3,000 전액이 charge_lots로 커버되니 locked_charged만 늘고 locked_reward는 0
        verify(walletMapper).updateLockedChargedBalance(10L, 3_000L);
        verify(walletMapper).updateLockedRewardBalance(10L, 0L);
        verify(chargeLotMapper).updateRemainingAmount(100L, 7_000L);
        verify(chargeLotAllocationMapper).insert(argThat(a ->
                a.getChargeLotId().equals(100L) && a.getWalletId().equals(10L)
                        && a.getReferenceId().equals(99L) && a.getAmount() == 3_000L));

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
                .chargedBalance(1_000L).rewardBalance(0L)
                .lockedChargedBalance(0L).lockedRewardBalance(0L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        assertThatThrownBy(() -> walletService.hold(userId, 5_000L, 99L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(walletMapper, never()).updateLockedChargedBalance(anyLong(), anyLong());
        verify(walletMapper, never()).updateLockedRewardBalance(anyLong(), anyLong());
        verify(chargeLotMapper, never()).selectRemainingByWalletIdOrderByChargedAtAsc(anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void hold_deductsAcrossMultipleLotsInFifoOrder() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(10_000L).rewardBalance(0L)
                .lockedChargedBalance(0L).lockedRewardBalance(0L)
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
        verify(walletMapper).updateLockedChargedBalance(10L, 1_200L); // 전액 charge_lots로 커버됨
        verify(walletMapper).updateLockedRewardBalance(10L, 0L);
    }

    @Test
    void hold_amountExceedsChargeLots_splitsBetweenLockedChargedAndLockedReward() {
        Long userId = 1L;
        // charged 2,000 + reward 8,000 = availableBalance 10,000 (5,000 홀딩엔 충분)
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(2_000L).rewardBalance(8_000L)
                .lockedChargedBalance(0L).lockedRewardBalance(0L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        ChargeLot lot = ChargeLot.builder().id(100L).walletId(10L).remainingAmount(2_000L).build();
        when(chargeLotMapper.selectRemainingByWalletIdOrderByChargedAtAsc(10L)).thenReturn(List.of(lot));

        walletService.hold(userId, 5_000L, 99L);

        // lot은 가진 만큼(2,000)만 차감되고, 나머지 3,000은 reward 몫
        verify(chargeLotMapper).updateRemainingAmount(100L, 0L);
        verify(walletMapper).updateLockedChargedBalance(10L, 2_000L);
        verify(walletMapper).updateLockedRewardBalance(10L, 3_000L);
    }

    @Test
    void cancel_fullyChargedHold_releasesLockedChargedAndRecordsRefundTransaction() {
        Long userId = 1L;
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(10_000L).rewardBalance(0L)
                .lockedChargedBalance(3_000L).lockedRewardBalance(0L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        ChargeLotAllocation allocation = ChargeLotAllocation.builder().id(1L).chargeLotId(100L).amount(3_000L).build();
        when(chargeLotAllocationMapper.selectUnreleasedByWalletIdAndReferenceId(10L, 99L))
                .thenReturn(List.of(allocation));

        walletService.cancel(userId, 3_000L, 99L);

        verify(chargeLotMapper).incrementRemainingAmount(100L, 3_000L); // releaseChargeLotsFifo 재사용 확인
        verify(walletMapper).updateLockedChargedBalance(10L, 0L); // 3,000 - 3,000(charged 몫)
        verify(walletMapper).updateLockedRewardBalance(10L, 0L);  // 0 - 0(reward 몫)

        ArgumentCaptor<CashTransaction> captor = ArgumentCaptor.forClass(CashTransaction.class);
        verify(cashTransactionMapper).insert(captor.capture());
        CashTransaction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(CashTransactionType.CHALLENGE_PRINCIPAL_REFUND);
        assertThat(saved.getAmount()).isEqualTo(3_000L);
        assertThat(saved.getReferenceId()).isEqualTo(99L);
        assertThat(saved.getBalanceAfter()).isEqualTo(10_000L); // availableBalance(7000) + 3000
    }

    @Test
    void cancel_mixedHold_splitsLockedReleaseBetweenChargedAndReward() {
        Long userId = 1L;
        // 5,000 홀딩 중 2,000은 charged, 3,000은 reward 몫이었던 상황
        Wallet wallet = Wallet.builder()
                .id(10L).userId(userId)
                .chargedBalance(2_000L).rewardBalance(8_000L)
                .lockedChargedBalance(2_000L).lockedRewardBalance(3_000L)
                .build();
        when(walletMapper.selectByUserIdForUpdate(userId)).thenReturn(wallet);

        ChargeLotAllocation allocation = ChargeLotAllocation.builder().id(1L).chargeLotId(100L).amount(2_000L).build();
        when(chargeLotAllocationMapper.selectUnreleasedByWalletIdAndReferenceId(10L, 99L))
                .thenReturn(List.of(allocation));

        walletService.cancel(userId, 5_000L, 99L);

        verify(chargeLotMapper).incrementRemainingAmount(100L, 2_000L);
        verify(walletMapper).updateLockedChargedBalance(10L, 0L); // 2,000 - 2,000
        verify(walletMapper).updateLockedRewardBalance(10L, 0L);  // 3,000 - 3,000(reward 몫)
    }

    @Test
    void releaseChargeLotsFifo_restoresExactlyTheRecordedAllocationsAndReturnsTotal() {
        ChargeLotAllocation allocation1 = ChargeLotAllocation.builder().id(1L).chargeLotId(100L).amount(3_000L).build();
        ChargeLotAllocation allocation2 = ChargeLotAllocation.builder().id(2L).chargeLotId(200L).amount(1_500L).build();
        when(chargeLotAllocationMapper.selectUnreleasedByWalletIdAndReferenceId(10L, 99L))
                .thenReturn(List.of(allocation1, allocation2));

        long restored = walletService.releaseChargeLotsFifo(10L, 99L);

        verify(chargeLotMapper).incrementRemainingAmount(100L, 3_000L);
        verify(chargeLotMapper).incrementRemainingAmount(200L, 1_500L);
        verify(chargeLotAllocationMapper).markReleased(eq(1L), any(LocalDateTime.class));
        verify(chargeLotAllocationMapper).markReleased(eq(2L), any(LocalDateTime.class));
        assertThat(restored).isEqualTo(4_500L);
    }

    @Test
    void releaseChargeLotsFifo_noAllocations_doesNothingAndReturnsZero() {
        // successUserIds에 같은 유저가 중복으로 들어와서 이미 처리된 경우도, 애초에 기록이 없는 경우도 여기 해당
        when(chargeLotAllocationMapper.selectUnreleasedByWalletIdAndReferenceId(10L, 99L)).thenReturn(List.of());

        long restored = walletService.releaseChargeLotsFifo(10L, 99L);

        verify(chargeLotMapper, never()).incrementRemainingAmount(anyLong(), anyLong());
        assertThat(restored).isZero();
    }

    @Test
    void sumChargeLotAllocations_sumsRecordedAllocationsWithoutMutatingAnything() {
        ChargeLotAllocation allocation1 = ChargeLotAllocation.builder().chargeLotId(100L).amount(3_000L).build();
        ChargeLotAllocation allocation2 = ChargeLotAllocation.builder().chargeLotId(200L).amount(1_500L).build();
        when(chargeLotAllocationMapper.selectByWalletIdAndReferenceId(10L, 99L))
                .thenReturn(List.of(allocation1, allocation2));

        long sum = walletService.sumChargeLotAllocations(10L, 99L);

        assertThat(sum).isEqualTo(4_500L);
        verify(chargeLotMapper, never()).incrementRemainingAmount(anyLong(), anyLong());
    }
}
