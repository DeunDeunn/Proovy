package com.deundeun.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naverpay")
public record NaverPayProperties(
        String apiDomain,
        String clientId,
        String clientSecret,
        String chainId,
        String shopId,
        String returnUrl
) {

    public String apiBaseUrl() {
        return "https://" + apiDomain + "/naverpay-partner/naverpay/payments";
    }

    public String settleBaseUrl() {
        return "https://" + apiDomain + "/naverpaysettle-payment/naverpaysettle";
    }
}
