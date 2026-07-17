package com.deundeun.certification.dto;

import lombok.Data;

// 신고 등록 요청
@Data
public class CreateReportRequest {
    private String targetType;  // POST / COMMENT
    private Long targetId;      // 신고 대상 id (인증글 또는 댓글)
    private String reason;      // SPAM / ABUSE / OBSCENE / FALSE_CERT / ETC
    private String detail;      // 상세 사유 (선택, 최대 500자)
}
