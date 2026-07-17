package com.deundeun.certification.dto;

import lombok.Getter;

import java.util.List;

// 관리자 신고 목록 응답 (offset 페이징)
@Getter
public class AdminReportListResponse {
    private final List<AdminReportItem> items;
    private final long totalCount;  // 필터 기준 전체 건수
    private final int page;         // 현재 페이지 (0-base)
    private final int size;         // 페이지 크기

    public AdminReportListResponse(List<AdminReportItem> items, long totalCount, int page, int size) {
        this.items = items;
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
    }
}
