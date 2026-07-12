package com.deundeun.pay.dto;

import com.deundeun.pay.enums.HostRevenueStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HostRevenueItem {
    private Long id;
    private Long challengeId;
    private Long amount;
    private HostRevenueStatus status;
    private LocalDateTime paidAt;
}
