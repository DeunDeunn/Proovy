package com.deundeun.pay.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletResponse {
    private Long chargedBalance;
    private Long rewardBalance;
    private Long lockedBalance;
    private Long availableBalance;
}
