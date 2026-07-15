package com.deundeun.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 결제 팝업 완료 후 returnUrl로 돌아온 프론트가 호출할 때 보내는 값.
 * merchantPayKey: 내가 충전 요청 시 발급해서 프론트에 준 값 (내 거래 조회용, 1차 힌트)
 * paymentId: returnUrl 리다이렉트로 네이버페이가 전달해준 결제번호 (진짜 승인 요청에 사용)
 * 최종 판단은 merchantPayKey/paymentId 둘 다가 아니라, paymentId로 네이버페이 서버에
 * 직접 승인 요청해서 받은 응답(admissionState, totalPayAmount 등)으로 한다.
 */
@Getter
public class NaverPayCallbackRequest {

    @NotBlank(message = "merchantPayKey가 필요합니다.")
    private String merchantPayKey;

    @NotBlank(message = "paymentId가 필요합니다.")
    private String paymentId;
}
