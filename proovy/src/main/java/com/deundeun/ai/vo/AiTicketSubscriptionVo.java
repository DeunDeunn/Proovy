package com.deundeun.ai.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTicketSubscriptionVo {

    private Long id;
    private Long hostId;
    private Long planId;
    private String planName;
    private Integer paidPrice;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
