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
public class ChargeLotAllocation {
    private Long id;
    private Long chargeLotId;
    private Long walletId;
    private Long referenceId;
    private Long amount;
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
}
