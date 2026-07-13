package com.deundeun.pay.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.dto.WithdrawableAmountResponse;
import com.deundeun.pay.service.WalletService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class WalletController {

    private static final int TRANSACTIONS_PAGE_SIZE = 10;

    private final WalletService walletService;

    @GetMapping("/api/wallets/me")
    public ApiResponse<WalletResponse> getMyWallet() {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(walletService.getWalletView(userId));
    }

    @GetMapping("/api/wallets/withdrawable-amount")
    public ApiResponse<WithdrawableAmountResponse> getWithdrawableAmount() {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(walletService.getWithdrawableAmount(userId));
    }

    @GetMapping("/api/wallets/transactions")
    public ApiResponse<TransactionHistoryResponse> getMyTransactions(
            @RequestParam(required = false) CashTransactionType type,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(walletService.getTransactionHistory(userId, type, page, TRANSACTIONS_PAGE_SIZE));
    }
}
