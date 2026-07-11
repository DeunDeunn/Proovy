package com.deundeun.pay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChargeRequest {

    @NotNull(message = "충전 금액을 입력해주세요.")
    private Long amount;
}
