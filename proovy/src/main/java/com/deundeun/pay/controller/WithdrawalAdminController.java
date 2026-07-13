package com.deundeun.pay.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.pay.dto.WithdrawalItem;
import com.deundeun.pay.dto.WithdrawalRejectRequest;
import com.deundeun.pay.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WithdrawalAdminController {

    private final WithdrawalService withdrawalService;

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
