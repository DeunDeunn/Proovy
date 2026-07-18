package com.deundeun.pay.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawableAmountResponse {
    private Long chargedWithdrawableAmount;
    private Long rewardWithdrawableAmount;
}
