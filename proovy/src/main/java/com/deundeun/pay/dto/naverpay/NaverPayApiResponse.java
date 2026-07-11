package com.deundeun.pay.dto.naverpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 네이버페이 API 공통 응답 포맷.
 * { "code": "Success", "message": "성공", "body": {} }
 *
 * code는 API마다 값 종류가 다르고 계속 추가될 수 있어(Success/Fail/InvalidMerchant/
 * TimeExpired/AlreadyOnGoing/AlreadyComplete/... ) enum으로 고정하지 않고 String으로 받는다.
 * 알려진 값은 NaverPayResultCode 상수 참고.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverPayApiResponse<T>(
        String code,
        String message,
        T body
) {
    public boolean isSuccess() {
        return NaverPayResultCode.SUCCESS.equals(code);
    }
}
