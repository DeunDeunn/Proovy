package com.deundeun.pay.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import com.deundeun.pay.dto.WithdrawalApplyRequest;
import com.deundeun.pay.dto.WithdrawalApplyResponse;
import com.deundeun.pay.dto.WithdrawalHistoryResponse;
import com.deundeun.pay.enums.WithdrawalStatus;
import com.deundeun.pay.service.WithdrawalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class WithdrawalController {

    private static final int WITHDRAWALS_PAGE_SIZE = 10;

    private final WithdrawalService withdrawalService;

    @PostMapping("/api/withdrawals")
    public ApiResponse<WithdrawalApplyResponse> applyWithdrawal(@Valid @RequestBody WithdrawalApplyRequest request){
        Long userId = CurrentUser.getUserId();
        WithdrawalApplyResponse response = withdrawalService.applyWithdrawal(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/api/withdrawals/me")
    public ApiResponse<WithdrawalHistoryResponse> getMyWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(withdrawalService.getWithdrawalHistory(userId, status, page, WITHDRAWALS_PAGE_SIZE));
    }
}
