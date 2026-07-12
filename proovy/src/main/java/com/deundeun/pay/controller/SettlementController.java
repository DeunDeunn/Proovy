package com.deundeun.pay.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import com.deundeun.pay.dto.HostRevenueHistoryResponse;
import com.deundeun.pay.dto.HostRevenueItem;
import com.deundeun.pay.dto.SettlementResultResponse;
import com.deundeun.pay.service.SettlementService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class SettlementController {

    private static final int HOST_REVENUES_PAGE_SIZE = 10;

    private final SettlementService settlementService;

    @GetMapping("/api/challenge-rooms/{challengeId}/settlement")
    public ApiResponse<SettlementResultResponse> getSettlementResults(@PathVariable Long challengeId){
        SettlementResultResponse response = settlementService.getSettlementResult(challengeId);
        return ApiResponse.success(response);
    }

    @GetMapping("/api/challenge-rooms/{challengeId}/settlement/host-revenue")
    public ApiResponse<HostRevenueItem> getHostRevenue(@PathVariable Long challengeId){
        return ApiResponse.success(settlementService.getHostRevenueByChallengeId(challengeId));
    }

    @GetMapping("/api/hosts/me/revenues")
    public ApiResponse<HostRevenueHistoryResponse> getHostRevenues(@RequestParam(defaultValue = "0") @Min(0) int page){
        Long hostId = CurrentUser.getUserId();
        return ApiResponse.success(settlementService.getHostRevenueHistory(hostId, page, HOST_REVENUES_PAGE_SIZE));
    }
}
