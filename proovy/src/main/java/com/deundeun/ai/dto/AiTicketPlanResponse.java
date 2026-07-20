package com.deundeun.ai.dto;

import com.deundeun.ai.vo.AiTicketPlanVo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiTicketPlanResponse {

    private Long id;
    private String name;
    private Integer durationDays;
    private Integer price;
    private String description;
    private Boolean active;

    public static AiTicketPlanResponse from(AiTicketPlanVo vo) {
        return AiTicketPlanResponse.builder()
                .id(vo.getId())
                .name(vo.getName())
                .durationDays(vo.getDurationDays())
                .price(vo.getPrice())
                .description(vo.getDescription())
                .active(vo.getActive())
                .build();
    }
}
