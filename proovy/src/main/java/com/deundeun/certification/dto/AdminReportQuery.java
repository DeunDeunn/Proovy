package com.deundeun.certification.dto;

import lombok.Getter;

// 관리자 신고 목록 조회 조건 (PENDING 고정 + targetType 필터 + offset 페이징)
@Getter
public class AdminReportQuery {
    private final String targetType;  // POST / COMMENT / null(전체)
    private final int limit;          // size
    private final int offset;         // page * size

    public AdminReportQuery(String targetType, int limit, int offset) {
        this.targetType = targetType;
        this.limit = limit;
        this.offset = offset;
    }
}
