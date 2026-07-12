package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.domain.CashTransactionType;
import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.TransactionItem;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.ChargeLotMapper;
import com.deundeun.pay.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService implements WalletHoldService {

    private final WalletMapper walletMapper;
    private final CashTransactionMapper cashTransactionMapper;
    private final ChargeLotMapper chargeLotMapper;

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
    public void updateRewardBalance(Long walletId, long newRewardBalance) {
        walletMapper.updateRewardBalance(walletId, newRewardBalance);
    }
    public void updateLockedBalance(Long walletId, long newLockedBalance) {
        walletMapper.updateLockedBalance(walletId, newLockedBalance);
    }

    /**
     * 참가비 등을 홀딩한다. charged_balance는 그대로 두고 locked_balance만 늘려서
     * 사용 가능 잔액(availableBalance)에서 amount만큼을 뺀다.
     * 동시에, 어느 충전 건에서 이 홀딩이 발생했는지 추적하기 위해 charge_lots를
     * 오래된 것부터(FIFO) amount만큼 차감한다 (사용자에게 노출되는 lot별 잔여액을 정확히 유지).
     * amount가 charge_lots 잔여 합계를 초과하는 부분은 reward_balance에서 온 것으로 보고
     * lot 차감 없이 넘어간다 (reward_balance는 lot으로 추적하지 않음).
     */
    @Override
    @Transactional
    public Long hold(Long userId, long amount, Long referenceId) {
        Wallet wallet = getWalletForUpdate(userId);

        if (wallet.getAvailableBalance() < amount) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        updateLockedBalance(wallet.getId(), wallet.getLockedBalance() + amount);
        deductChargeLotsFifo(wallet.getId(), amount);

        CashTransaction transaction = CashTransaction.builder()
                .walletId(wallet.getId())
                .type(CashTransactionType.CHALLENGE_HOLD)
                .amount(amount)
                .balanceAfter(wallet.getAvailableBalance() - amount)
                .status(CashTransactionStatus.COMPLETED)
                .referenceId(referenceId)
                .build();
        cashTransactionMapper.insert(transaction);

        return transaction.getId();
    }

    private void deductChargeLotsFifo(Long walletId, long amount) {
        long remaining = amount;
        for (ChargeLot lot : chargeLotMapper.selectRemainingByWalletIdOrderByChargedAtAsc(walletId)) {
            if (remaining <= 0) {
                break;
            }
            long deduct = Math.min(lot.getRemainingAmount(), remaining);
            chargeLotMapper.updateRemainingAmount(lot.getId(), lot.getRemainingAmount() - deduct);
            remaining -= deduct;
        }
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
