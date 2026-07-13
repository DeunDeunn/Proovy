package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.domain.WithdrawalRequest;
import com.deundeun.pay.dto.WithdrawalApplyRequest;
import com.deundeun.pay.dto.WithdrawalApplyResponse;
import com.deundeun.pay.dto.WithdrawalHistoryResponse;
import com.deundeun.pay.dto.WithdrawalItem;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.enums.SourceType;
import com.deundeun.pay.enums.WithdrawalStatus;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.WithdrawalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private static final BigDecimal CHARGED_FEE_RATE = BigDecimal.valueOf(0.05);
    private static final BigDecimal REWARD_FEE_RATE = BigDecimal.valueOf(0.01);
    private static final long MIN_REWARD_WITHDRAWAL_AMOUNT = 5_000L;

    private final WithdrawalMapper withdrawalMapper;
    private final WalletService walletService;
    private final CashTransactionMapper cashTransactionMapper;

    @Transactional
    public WithdrawalApplyResponse applyWithdrawal(Long userId, WithdrawalApplyRequest request) {
        Wallet wallet = walletService.getWalletForUpdate(userId);
        long amount = request.getAmount();
        SourceType sourceType = request.getSourceType();

        if (sourceType == SourceType.REWARD) {
            if (amount < MIN_REWARD_WITHDRAWAL_AMOUNT) {
                throw new ApiException(ErrorCode.INVALID_WITHDRAWAL_AMOUNT);
            }
            if (wallet.getRewardBalance() < amount) {
                throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
            }
            walletService.updateRewardBalance(wallet.getId(), wallet.getRewardBalance() - amount);
        } else {
            long chargedWithdrawable = walletService.getChargedWithdrawableAmount(wallet.getId());
            if (chargedWithdrawable < amount) {
                throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
            }
            walletService.updateChargedBalance(wallet.getId(), wallet.getChargedBalance() - amount);
            walletService.deductChargeLotsFifo(wallet.getId(), amount);
        }

        long feeAmount = calculateFee(sourceType, amount);
        long netTransferAmount = amount - feeAmount;

        WithdrawalRequest withdrawalRequest = WithdrawalRequest.builder()
                .walletId(wallet.getId())
                .sourceType(sourceType)
                .amount(amount)
                .feeAmount(feeAmount)
                .netTransferAmount(netTransferAmount)
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .status(WithdrawalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        withdrawalMapper.insert(withdrawalRequest);

        cashTransactionMapper.insert(CashTransaction.builder()
                .walletId(wallet.getId())
                .type(CashTransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceAfter(wallet.getAvailableBalance() - amount)
                .status(CashTransactionStatus.COMPLETED)
                .referenceId(withdrawalRequest.getId())
                .build());

        return WithdrawalApplyResponse.builder()
                .withdrawalRequestId(withdrawalRequest.getId())
                .sourceType(withdrawalRequest.getSourceType())
                .amount(withdrawalRequest.getAmount())
                .feeAmount(withdrawalRequest.getFeeAmount())
                .netTransferAmount(withdrawalRequest.getNetTransferAmount())
                .status(withdrawalRequest.getStatus())
                .requestedAt(withdrawalRequest.getRequestedAt())
                .build();
    }

    @Transactional
    public WithdrawalHistoryResponse getWithdrawalHistory(Long userId, WithdrawalStatus status, int page, int size) {
        Wallet wallet = walletService.getOrCreateWallet(userId);

        List<WithdrawalItem> content = withdrawalMapper.selectByWalletId(wallet.getId(), status, page * size, size);
        long totalElements = withdrawalMapper.countByWalletId(wallet.getId(), status);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return WithdrawalHistoryResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private long calculateFee(SourceType sourceType, long amount) {
        BigDecimal rate = sourceType == SourceType.REWARD ? REWARD_FEE_RATE : CHARGED_FEE_RATE;
        return BigDecimal.valueOf(amount)
                .multiply(rate)
                .setScale(0, RoundingMode.FLOOR)
                .longValueExact();
    }
}
