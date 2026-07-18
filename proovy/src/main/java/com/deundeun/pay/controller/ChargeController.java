package com.deundeun.pay.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import com.deundeun.pay.dto.ChargeRequest;
import com.deundeun.pay.dto.ChargeResponse;
import com.deundeun.pay.dto.NaverPayCallbackRequest;
import com.deundeun.pay.dto.NaverPayCallbackResponse;
import com.deundeun.pay.service.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;

    @PostMapping("/api/wallets/charge")
    public ApiResponse<ChargeResponse> charge(@Valid @RequestBody ChargeRequest request) {
        Long userId = CurrentUser.getUserId();
        ChargeResponse response = chargeService.requestCharge(userId, request.getAmount());
        return ApiResponse.success(response);
    }

    /**
     * oPay.open() 결제 팝업 완료 후, returnUrl로 돌아온 프론트가 호출하는 API.
     * 여기서 네이버페이 결제 승인(apply) API를 서버 대 서버로 직접 호출해서 결과를 검증한다.
     */
    @PostMapping("/api/payments/naverpay/callback")
    public ApiResponse<NaverPayCallbackResponse> naverPayCallback(@Valid @RequestBody NaverPayCallbackRequest callback) {
        NaverPayCallbackResponse response = chargeService.handlePaymentCompleted(callback);
        return ApiResponse.success(response);
    }
}
