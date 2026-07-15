package com.deundeun.pay.dto;

import com.deundeun.pay.enums.SettlementStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SettlementResultResponse {
    private Long challengeId;
    private Integer totalParticipantCount;
    private Integer successUserCount;
    private Integer failUserCount;
    private Long failurePool;
    private BigDecimal participantShareRate;
    private BigDecimal platformFeeRate;
    private BigDecimal hostFeeRate;
    private Long participantShareAmount;
    private Long platformFeeAmount;
    private Long hostFeeAmount;
    private Long profitPerUser;
    private Boolean isHostDisqualified;
    private SettlementStatus status;
    private LocalDateTime settledAt;
}
