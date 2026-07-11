package com.deundeun.pay.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import com.deundeun.pay.domain.CashTransactionType;
import com.deundeun.pay.dto.TransactionHistoryResponse;
import com.deundeun.pay.dto.WalletResponse;
import com.deundeun.pay.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/api/wallets/me")
    public ApiResponse<WalletResponse> getMyWallet() {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(walletService.getWalletView(userId));
    }

    @GetMapping("/api/wallets/transactions")
    public ApiResponse<TransactionHistoryResponse> getMyTransactions(
            @RequestParam(required = false) CashTransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(walletService.getTransactionHistory(userId, type, page, size));
    }
}
