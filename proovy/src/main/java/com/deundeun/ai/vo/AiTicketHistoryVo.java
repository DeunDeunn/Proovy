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
public class AiTicketHistoryVo {

    private Long id;
    private Long hostId;
    private Long subscriptionId;
    private String type;
    private LocalDateTime createdAt;
}
