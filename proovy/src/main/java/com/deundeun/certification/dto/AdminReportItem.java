package com.deundeun.certification.dto;

import com.deundeun.certification.enums.ReportReason;
import com.deundeun.certification.enums.ReportStatus;
import com.deundeun.certification.enums.ReportTargetType;
import lombok.Data;

import java.time.LocalDateTime;

// 관리자 신고 목록의 한 건
@Data
public class AdminReportItem {
    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReason reason;
    private String detail;
    private ReportStatus status;
    private Long reporterId;         // 신고자 user id
    private String reporterNickname; // 신고자 닉네임
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
