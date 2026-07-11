package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.CashTransactionStatus;
import com.deundeun.pay.domain.CashTransactionType;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.WalletMapper;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private CashTransactionMapper cashTransactionMapper;

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
        assertThat(response.getContent().get(0).getId()).isEqualTo(5L);
        assertThat(response.getTotalElements()).isEqualTo(23L);
        assertThat(response.getTotalPages()).isEqualTo(3); // ceil(23/10)
    }
}
