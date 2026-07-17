package com.deundeun.certification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// 생성된 신고 id를 돌려주는용
@Data
@AllArgsConstructor
public class CreateReportResponse {
    private Long reportId;   // 생성된 신고 id
}
