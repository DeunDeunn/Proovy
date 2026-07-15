package com.deundeun.pay.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.pay.dto.WithdrawalHistoryResponse;
import com.deundeun.pay.dto.WithdrawalItem;
import com.deundeun.pay.dto.WithdrawalRejectRequest;
import com.deundeun.pay.enums.WithdrawalStatus;
import com.deundeun.pay.service.WithdrawalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class WithdrawalAdminController {

    private static final int WITHDRAWALS_PAGE_SIZE = 10;

    private final WithdrawalService withdrawalService;

    @GetMapping("/api/admin/withdrawals")
    public ApiResponse<WithdrawalHistoryResponse> getAllWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ApiResponse.success(withdrawalService.getAllWithdrawals(status, page, WITHDRAWALS_PAGE_SIZE));
    }

    @PostMapping("/api/admin/withdrawals/{withdrawalId}/complete")
    public ApiResponse<WithdrawalItem> completeWithdrawal(@PathVariable Long withdrawalId) {
        return ApiResponse.success(withdrawalService.completeWithdrawal(withdrawalId));
    }

    @PostMapping("/api/admin/withdrawals/{withdrawalId}/reject")
    public ApiResponse<WithdrawalItem> rejectWithdrawal(@PathVariable Long withdrawalId,
                                                         @Valid @RequestBody WithdrawalRejectRequest request) {
        return ApiResponse.success(withdrawalService.rejectWithdrawal(withdrawalId, request.getRejectReason()));
    }
}
