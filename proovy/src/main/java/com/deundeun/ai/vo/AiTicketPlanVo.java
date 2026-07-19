package com.deundeun.ai.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTicketPlanVo {

    private Long id;
    private String name;
    private Integer durationDays;
    private Integer price;
    private String description;
    private Boolean active;
}
