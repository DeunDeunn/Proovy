package com.deundeun.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTicketPurchaseRequest {

    private Long planId;

    @JsonCreator
    public AiTicketPurchaseRequest(
            @JsonProperty("planId") Long planId
    ) {
        this.planId = planId;
    }
}
