package com.deundeun.pay.dto.naverpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 결제 승인 API 응답의 body.detail. 실제 사용하는 필드 위주로 매핑하고,
 * 나머지(카드/은행 정보 등)는 필요해지면 추가한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverPayPaymentDetail(
        String paymentId,
        String payHistId,
        String merchantId,
        String merchantPayKey,
        String admissionState,
        Long totalPayAmount,
        Long taxScopeAmount,
        Long taxExScopeAmount,
        String productName
) {
    private static final String ADMISSION_SUCCESS = "SUCCESS";

    public boolean isAdmitted() {
        return ADMISSION_SUCCESS.equals(admissionState);
    }
}
