package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.ChargeLotAllocation;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.TransactionItem;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.dto.WithdrawableAmountResponse;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.ChargeLotAllocationMapper;
import com.deundeun.pay.mapper.ChargeLotMapper;
import com.deundeun.pay.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService implements WalletHoldService {

    private final WalletMapper walletMapper;
    private final CashTransactionMapper cashTransactionMapper;
    private final ChargeLotMapper chargeLotMapper;
    private final ChargeLotAllocationMapper chargeLotAllocationMapper;

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
     * FOR UPDATE 락은 트랜잭션이 끝날 때까지만 유지되므로, 호출부가 트랜잭션을 깜빡해도
     * 락이 조용히 무력화되지 않도록 이 메서드 자체에도 방어적으로 @Transactional을 건다
     * (기본 전파 방식이 REQUIRED라, 이미 트랜잭션 안에서 호출되면 거기에 그대로 편승한다).
     */
    @Transactional
    public Wallet getWalletByIdForUpdate(Long walletId) {
        return walletMapper.selectByIdForUpdate(walletId);
    }

    public void updateChargedBalance(Long walletId, long newChargedBalance) {
        walletMapper.updateChargedBalance(walletId, newChargedBalance);
    }
    public void updateRewardBalance(Long walletId, long newRewardBalance) {
        walletMapper.updateRewardBalance(walletId, newRewardBalance);
    }
    public void updateLockedChargedBalance(Long walletId, long newLockedChargedBalance) {
        walletMapper.updateLockedChargedBalance(walletId, newLockedChargedBalance);
    }
    public void updateLockedRewardBalance(Long walletId, long newLockedRewardBalance) {
        walletMapper.updateLockedRewardBalance(walletId, newLockedRewardBalance);
    }

    /**
     * 참가비 등을 홀딩한다. charged_balance/reward_balance는 그대로 두고, amount를 두 출처로 나눠
     * locked_charged_balance/locked_reward_balance에 각각 반영한다 (사용 가능 잔액에서 amount만큼 빠짐).
     * charged 몫은 charge_lots를 오래된 것부터(FIFO) 차감해서 추적하고, 그걸로 못 채우는 나머지는
     * reward에서 온 것으로 보고 locked_reward_balance에 반영한다 (reward_balance는 lot으로 추적하지 않음).
     */
    @Override
    @Transactional
    public Long hold(Long userId, long amount, Long referenceId) {
        Wallet wallet = getWalletForUpdate(userId);

        boolean alreadyHeld = cashTransactionMapper.selectByWalletIdAndReferenceIdAndType(
                wallet.getId(), referenceId, CashTransactionType.CHALLENGE_HOLD) != null;
        if (alreadyHeld) {
            throw new ApiException(ErrorCode.HOLD_ALREADY_EXISTS);
        }

        if (wallet.getAvailableBalance() < amount) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        long chargedPortion = holdChargeLotsFifo(wallet.getId(), amount, referenceId);
        long rewardPortion = amount - chargedPortion;
        updateLockedChargedBalance(wallet.getId(), wallet.getLockedChargedBalance() + chargedPortion);
        updateLockedRewardBalance(wallet.getId(), wallet.getLockedRewardBalance() + rewardPortion);

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

    /**
     * 챌린지 시작 전 취소처럼, 손실 없이 홀딩을 전액 그대로 돌려준다.
     * 정산 성공 시 참가비를 돌려주는 것과 로직이 동일해서 releaseChargeLotsFifo를 그대로 재사용한다.
     * amount는 원본 CHALLENGE_HOLD 거래 기록과 대조해 검증하고, CHALLENGE_PRINCIPAL_REFUND가
     * 이미 있으면 중복 취소 요청으로 보고 거부한다 (재시도로 인한 잠금 잔액 이중 차감/환불 중복 기록 방지).
     */
    @Override
    @Transactional
    public void cancel(Long userId, long amount, Long referenceId) {
        Wallet wallet = getWalletForUpdate(userId);

        CashTransaction holdTransaction = cashTransactionMapper.selectByWalletIdAndReferenceIdAndType(
                wallet.getId(), referenceId, CashTransactionType.CHALLENGE_HOLD);
        if (holdTransaction == null) {
            throw new ApiException(ErrorCode.HOLD_NOT_FOUND);
        }
        if (holdTransaction.getAmount() != amount) {
            throw new ApiException(ErrorCode.INVALID_CANCEL_AMOUNT);
        }
        boolean alreadyCancelled = cashTransactionMapper.selectByWalletIdAndReferenceIdAndType(
                wallet.getId(), referenceId, CashTransactionType.CHALLENGE_PRINCIPAL_REFUND) != null;
        if (alreadyCancelled) {
            throw new ApiException(ErrorCode.HOLD_ALREADY_CANCELLED);
        }

        long chargedPortion = releaseChargeLotsFifo(wallet.getId(), referenceId);
        long rewardPortion = amount - chargedPortion;
        updateLockedChargedBalance(wallet.getId(), wallet.getLockedChargedBalance() - chargedPortion);
        updateLockedRewardBalance(wallet.getId(), wallet.getLockedRewardBalance() - rewardPortion);

        cashTransactionMapper.insert(CashTransaction.builder()
                .walletId(wallet.getId())
                .type(CashTransactionType.CHALLENGE_PRINCIPAL_REFUND)
                .amount(amount)
                .balanceAfter(wallet.getAvailableBalance() + amount)  // 홀딩 해제로 늘어난 사용가능잔액
                .status(CashTransactionStatus.COMPLETED)
                .referenceId(referenceId)
                .build());
    }

    /**
     * 출금처럼 영구적으로 빠져나가는 돈일 때 사용. 복구할 일이 없으니 어느 lot에서 뺐는지 따로 기록하지 않는다.
     */
    public void deductChargeLotsFifo(Long walletId, long amount) {
        long remaining = amount;
        for (ChargeLot lot : chargeLotMapper.selectRemainingByWalletIdOrderByChargedAtAsc(walletId)) {
            if (remaining <= 0) {
                break;
            }
            long deduct = Math.min(lot.getRemainingAmount(), remaining);
            decrementRemainingAmount(lot.getId(), deduct);
            remaining -= deduct;
        }
    }

    /**
     * 참가비 홀딩처럼, 나중에 정산 결과에 따라 charge_lots를 복구해야 할 수도 있는 경우 사용.
     * FIFO 차감과 동시에 어느 lot에서 얼마를 가져갔는지 charge_lot_allocations에 기록해두고,
     * 실제로 lot에서 차감된 총액(= amount 중 charged 몫)을 반환한다.
     */
    public long holdChargeLotsFifo(Long walletId, long amount, Long referenceId) {
        long remaining = amount;
        for (ChargeLot lot : chargeLotMapper.selectRemainingByWalletIdOrderByChargedAtAsc(walletId)) {
            if (remaining <= 0) {
                break;
            }
            long deduct = Math.min(lot.getRemainingAmount(), remaining);
            decrementRemainingAmount(lot.getId(), deduct);
            chargeLotAllocationMapper.insert(ChargeLotAllocation.builder()
                    .chargeLotId(lot.getId())
                    .walletId(walletId)
                    .referenceId(referenceId)
                    .amount(deduct)
                    .build());
            remaining -= deduct;
        }
        return amount - remaining;
    }

    /**
     * lot의 remaining_amount를 DB가 현재 값 기준으로 상대적으로 차감하게 한다(자바에서 계산한
     * 절대값으로 덮어쓰지 않음). 지갑 락 덕분에 지금은 항상 성공해야 하는 호출이라, 0 row가
     * 갱신되면(잔여액 부족 등으로 조건에 걸린 경우) 락 보호가 깨진 것으로 보고 즉시 실패시킨다.
     */
    private void decrementRemainingAmount(Long lotId, long deduct) {
        int updated = chargeLotMapper.decrementRemainingAmount(lotId, deduct);
        if (updated == 0) {
            throw new ApiException(ErrorCode.DATABASE_ERROR);
        }
    }

    /**
     * 정산 성공 등으로 홀딩이 완전히 해제(참가비 그대로 반환)될 때, holdChargeLotsFifo가 남겨둔
     * lot별 차감 기록을 찾아 정확히 그 lot들에 remaining_amount를 되돌리고, 복구한 총액(= charged 몫)을 반환한다.
     */
    public long releaseChargeLotsFifo(Long walletId, Long referenceId) {
        long totalRestored = 0;
        LocalDateTime releasedAt = LocalDateTime.now();
        // released_at IS NULL인 것만 대상으로 해서, successUserIds에 같은 유저가 중복으로 들어와도
        // 같은 홀딩이 두 번 복구되지 않는다.
        for (ChargeLotAllocation allocation : chargeLotAllocationMapper.selectUnreleasedByWalletIdAndReferenceId(walletId, referenceId)) {
            chargeLotMapper.incrementRemainingAmount(allocation.getChargeLotId(), allocation.getAmount());
            chargeLotAllocationMapper.markReleased(allocation.getId(), releasedAt);
            totalRestored += allocation.getAmount();
        }
        return totalRestored;
    }

    /**
     * 정산 실패 등으로 홀딩이 영구 손실 처리될 때 사용. charge_lots는 이미 홀딩 시점에 깎인 채로 둬야
     * 맞으므로(돈이 진짜 사라졌으니) 복구하지 않고, 이 홀딩의 charged 몫이 얼마였는지만 조회한다.
     */
    public long sumChargeLotAllocations(Long walletId, Long referenceId) {
        return chargeLotAllocationMapper.selectByWalletIdAndReferenceId(walletId, referenceId).stream()
                .mapToLong(ChargeLotAllocation::getAmount)
                .sum();
    }

    @Transactional
    public WalletResponse getWalletView(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return WalletResponse.builder()
                .chargedBalance(wallet.getChargedBalance())
                .rewardBalance(wallet.getRewardBalance())
                .lockedChargedBalance(wallet.getLockedChargedBalance())
                .lockedRewardBalance(wallet.getLockedRewardBalance())
                .availableBalance(wallet.getAvailableBalance())
                .build();
    }

    @Transactional
    public WithdrawableAmountResponse getWithdrawableAmount(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return WithdrawableAmountResponse.builder()
                .chargedWithdrawableAmount(getChargedWithdrawableAmount(wallet.getId()))
                .rewardWithdrawableAmount(wallet.getUnlockedRewardBalance())
                .build();
    }

    /**
     * 충전일로부터 7일이 지나 지금 당장 출금 가능한 charged_balance 부분.
     * 출금 신청 시 잔액 검증에도 이 값을 그대로 사용한다.
     */
    public long getChargedWithdrawableAmount(Long walletId) {
        return chargeLotMapper.sumWithdrawableRemainingByWalletId(walletId);
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
