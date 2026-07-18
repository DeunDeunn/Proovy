package com.deundeun.certification.dto;

import lombok.Getter;
import lombok.Setter;

// 신고 INSERT용 파라미터 (id는 생성 후 채워짐)
@Getter
@Setter
public class CreateReportSqlParam {
    private Long id;           // insert 후 생성된 신고 id
    private Long userId;       // 신고자
    private String targetType; // POST / COMMENT
    private Long targetId;     // 대상 id
    private String reason;     // 신고 사유
    private String detail;     // 상세 사유 (nullable)

    public CreateReportSqlParam(Long userId, String targetType, Long targetId,
                                String reason, String detail) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
    }
}
