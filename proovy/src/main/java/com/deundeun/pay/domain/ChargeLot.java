package com.deundeun.pay.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeLot {
    private Long id;
    private Long walletId;
    private Long amount;
    private Long remainingAmount;
    private LocalDateTime chargedAt;
    private LocalDateTime withdrawableAt;
}
