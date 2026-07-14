package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.mapper.CashTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService implements WalletTicketService {

    private final WalletService walletService;
    private final CashTransactionMapper cashTransactionMapper;

    @Override
    @Transactional
    public Long purchase(Long userId, long amount, Long referenceId) {
        if (amount <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        Wallet wallet = walletService.getWalletForUpdate(userId);

        boolean alreadyPurchased = cashTransactionMapper.selectByWalletIdAndReferenceIdAndType(
                wallet.getId(), referenceId, CashTransactionType.AI_TICKET_PURCHASE) != null;
        if (alreadyPurchased) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_PURCHASED);
        }

        if (wallet.getUnlockedChargedBalance() < amount) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        walletService.updateChargedBalance(wallet.getId(), wallet.getChargedBalance() - amount);
        walletService.deductChargeLotsFifo(wallet.getId(), amount); // 환불이 없어 복구 추적 불필요, 영구 소진

        CashTransaction transaction = CashTransaction.builder()
                .walletId(wallet.getId())
                .type(CashTransactionType.AI_TICKET_PURCHASE)
                .amount(amount)
                .balanceAfter(wallet.getChargedBalance() - amount)
                .status(CashTransactionStatus.COMPLETED)
                .referenceId(referenceId)
                .build();
        cashTransactionMapper.insert(transaction);

        return transaction.getId();
    }
}
