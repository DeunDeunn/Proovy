package com.deundeun.pay.domain;

import com.deundeun.pay.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {
    private Long id;
    private Long challengeId;
    private Long perPersonFee;
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
    private Long roundingRemainder;
    private Boolean isHostDisqualified;
    private SettlementStatus status;
    private LocalDateTime settledAt;
}
