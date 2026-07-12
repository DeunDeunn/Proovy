package com.deundeun.pay.domain;

import com.deundeun.pay.enums.HostRevenueStatus;
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
public class HostRevenue {
    private Long id;
    private Long hostId;
    private Long challengeId;
    private Long settlementId;
    private Long amount;
    private HostRevenueStatus status;
    private LocalDateTime paidAt;
}
