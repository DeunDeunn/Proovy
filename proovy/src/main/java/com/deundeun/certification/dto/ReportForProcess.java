package com.deundeun.certification.dto;

import com.deundeun.certification.enums.ReportStatus;
import com.deundeun.certification.enums.ReportTargetType;
import lombok.Data;

// 관리자 처리/기각 시 상태 확인 + 삭제 대상 식별용
@Data
public class ReportForProcess {
    private Long id;
    private ReportStatus status;
    private ReportTargetType targetType;  // POST / COMMENT
    private Long targetId;                // 대상 id
}
