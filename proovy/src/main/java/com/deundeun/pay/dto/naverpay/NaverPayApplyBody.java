package com.deundeun.pay.dto.naverpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverPayApplyBody(
        String paymentId,
        NaverPayPaymentDetail detail
) {
}
