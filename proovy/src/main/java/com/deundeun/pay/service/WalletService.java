package com.deundeun.pay.service;

import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.CashTransactionType;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.TransactionItem;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletMapper walletMapper;
    private final CashTransactionMapper cashTransactionMapper;

    /**
     * 지갑이 없으면 생성하고, 있으면 그대로 조회한다.
     * INSERT ... ON CONFLICT DO NOTHING을 사용해 동시 요청에도 안전하다.
     */
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        walletMapper.insertIfAbsent(userId);
        return walletMapper.selectByUserId(userId);
    }

    /**
     * 잔액 변경 트랜잭션 안에서 사용. 지갑 row를 잠그고 조회한다.
     */
    @Transactional
    public Wallet getWalletForUpdate(Long userId) {
        walletMapper.insertIfAbsent(userId);
        return walletMapper.selectByUserIdForUpdate(userId);
    }

    /**
     * walletId를 이미 알고 있을 때(PG 콜백 등) 지갑 row를 잠그고 조회한다.
     */
    public Wallet getWalletByIdForUpdate(Long walletId) {
        return walletMapper.selectByIdForUpdate(walletId);
    }

    public void updateChargedBalance(Long walletId, long newChargedBalance) {
        walletMapper.updateChargedBalance(walletId, newChargedBalance);
    }

    @Transactional
    public WalletResponse getWalletView(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return WalletResponse.builder()
                .chargedBalance(wallet.getChargedBalance())
                .rewardBalance(wallet.getRewardBalance())
                .lockedBalance(wallet.getLockedBalance())
                .availableBalance(wallet.getAvailableBalance())
                .build();
    }

    @Transactional
    public TransactionHistoryResponse getTransactionHistory(Long userId, CashTransactionType type, int page, int size) {
        Wallet wallet = getOrCreateWallet(userId);

        List<CashTransaction> transactions = cashTransactionMapper.selectByWalletId(
                wallet.getId(), type, page * size, size);
        long totalElements = cashTransactionMapper.countByWalletId(wallet.getId(), type);

        List<TransactionItem> content = transactions.stream()
                .map(t -> TransactionItem.builder()
                        .id(t.getId())
                        .type(t.getType())
                        .amount(t.getAmount())
                        .balanceAfter(t.getBalanceAfter())
                        .referenceId(t.getReferenceId())
                        .status(t.getStatus())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return TransactionHistoryResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
