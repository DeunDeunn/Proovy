package com.deundeun.certification.enums;

// 신고 처리 상태
public enum ReportStatus {

    PENDING,     // 접수(처리 대기)
    PROCESSED,   // 처리 완료 (신고 인정 → 조치)
    REJECTED     // 기각 (신고 무효 → 조치 안 함)

}
