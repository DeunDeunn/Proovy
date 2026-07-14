package com.deundeun.pay.dto;

import com.deundeun.pay.enums.CashTransactionStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 프론트엔드가 네이버페이 JS SDK(oPay.open())를 호출할 때 그대로 넘길 파라미터들.
 * clientId/chainId는 SDK 초기화(Naver.Pay.create()) 시점에 필요한 프론트 자체 설정값이라
 * 여기 포함하지 않는다.
 */
@Getter
@Builder
public class ChargeResponse {
    private Long chargeTransactionId;
    private String merchantPayKey;
    private String productName;
    private Integer productCount;
    private Long totalPayAmount;
    private Long taxScopeAmount;
    private Long taxExScopeAmount;
    private String returnUrl;
    private CashTransactionStatus status;
}
