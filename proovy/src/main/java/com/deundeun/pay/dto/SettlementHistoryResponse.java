package com.deundeun.pay.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SettlementHistoryResponse {
    private List<SettlementHistoryItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
