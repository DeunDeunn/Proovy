package com.deundeun.certification.service;

import com.deundeun.certification.dto.AdminReportItem;
import com.deundeun.certification.dto.AdminReportListResponse;
import com.deundeun.certification.dto.AdminReportQuery;
import com.deundeun.certification.dto.CreateReportRequest;
import com.deundeun.certification.dto.CreateReportSqlParam;
import com.deundeun.certification.dto.ReportForProcess;
import com.deundeun.certification.enums.ReportReason;
import com.deundeun.certification.enums.ReportStatus;
import com.deundeun.certification.enums.ReportTargetType;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.certification.mapper.ReportMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReportService {

    private static final int MAX_DETAIL_LENGTH = 500;  // reports.detail VARCHAR(500)
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ReportMapper reportMapper;
    private final CertificationMapper certificationMapper;
    private final CertificationService certificationService;

    // 신고 등록. 읽을 수 있는 대상에만, 유저당 1회.
    public Long createReport(Long userId, CreateReportRequest request) {
        // 1. 입력값 검증 (대상 종류·사유는 필수, 상세는 길이 제한)
        ReportTargetType targetType = parseTargetType(request.getTargetType());
        ReportReason reason = parseReason(request.getReason());
        if (request.getTargetId() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        String detail = request.getDetail();
        if (detail != null && detail.length() > MAX_DETAIL_LENGTH) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        // 2. 대상 존재 + 읽기 권한 확인
        assertTargetReadable(targetType, request.getTargetId(), userId);

        // 3. 등록 (UNIQUE 충돌 시 0행 → 중복 신고)
        CreateReportSqlParam param = new CreateReportSqlParam(
                userId, targetType.name(), request.getTargetId(), reason.name(), detail);
        int inserted = reportMapper.insertReport(param);
        if (inserted == 0) {
            throw new ApiException(ErrorCode.ALREADY_REPORTED);
        }
        return param.getId();
    }

    // 대상이 존재하고 신고자가 읽을 수 있는지 검사 (없거나 못 읽으면 숨김)
    private void assertTargetReadable(ReportTargetType targetType, Long targetId, Long userId) {
        if (targetType == ReportTargetType.POST) {
            // 인증글: 존재 + 읽기 게이트 재사용
            certificationService.assertPostReadable(targetId, userId);
        } else {
            // 댓글: 소속 인증글을 읽을 수 있어야 신고 가능
            Long postId = reportMapper.findPostIdByCommentId(targetId);
            if (postId == null) {
                throw new ApiException(ErrorCode.REPORT_TARGET_NOT_FOUND);
            }
            certificationService.assertPostReadable(postId, userId);
        }
    }

    // 관리자 신고 목록 (PENDING만, targetType 필터 + offset 페이징)
    public AdminReportListResponse getReportsForAdmin(Long userId, String targetType,
                                                      Integer page, Integer size) {
        assertAdmin(userId);

        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null) ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        int maxPage = Integer.MAX_VALUE / safeSize;   // offset 오버플로 방지 (page * size가 int 범위 초과 시 음수 랩)
        if (safePage > maxPage) {
            safePage = maxPage;
        }
        int offset = safePage * safeSize;

        AdminReportQuery query = new AdminReportQuery(
                normalizeTargetType(targetType), safeSize, offset);

        List<AdminReportItem> items = reportMapper.findReportsForAdmin(query);
        long totalCount = reportMapper.countReportsForAdmin(query);
        return new AdminReportListResponse(items, totalCount, safePage, safeSize);
    }

    // 관리자 신고 인정 처리 (PENDING → PROCESSED): 대상 삭제 + 같은 대상의 다른 PENDING 신고도 일괄 처리
    @Transactional
    public void processReport(Long userId, Long reportId) {
        ReportForProcess report = loadPendingReport(userId, reportId);

        // 1. 신고 대상(게시글/댓글) soft delete
        if (report.getTargetType() == ReportTargetType.POST) {
            certificationMapper.softDeletePost(report.getTargetId());
        } else {
            reportMapper.softDeleteComment(report.getTargetId());
        }

        // 2. 같은 대상의 PENDING 신고를 모두 PROCESSED로 (중복 신고 일괄 처리)
        //    이 신고가 PENDING임을 위에서 확인했으므로 최소 1행. 0행이면 동시 처리로 이미 종료된 것
        int updated = reportMapper.markProcessedByTarget(
                report.getTargetType().name(), report.getTargetId());
        if (updated == 0) {
            throw new ApiException(ErrorCode.ALREADY_PROCESSED_REPORT);
        }
    }

    // 관리자 신고 기각 (PENDING → REJECTED, 신고 무효). 대상은 그대로 두고 이 신고만 종료
    public void rejectReport(Long userId, Long reportId) {
        loadPendingReport(userId, reportId);
        // 동시 요청 방어: PENDING일 때만 갱신 → 0행이면 이미 처리됨
        int updated = reportMapper.markRejected(reportId);
        if (updated == 0) {
            throw new ApiException(ErrorCode.ALREADY_PROCESSED_REPORT);
        }
    }

    // 관리자 권한 + 신고 존재 + PENDING 상태 검증 후 신고 반환 (처리/기각 공통)
    private ReportForProcess loadPendingReport(Long userId, Long reportId) {
        assertAdmin(userId);
        ReportForProcess report = reportMapper.findReportForProcess(reportId);
        if (report == null) {
            throw new ApiException(ErrorCode.REPORT_NOT_FOUND);
        }
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ApiException(ErrorCode.ALREADY_PROCESSED_REPORT);
        }
        return report;
    }

    private void assertAdmin(Long userId) {
        if (certificationMapper.isAdmin(userId) <= 0) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    // ---- 입력 파싱 (요청 본문: 잘못되면 400) ----

    private ReportTargetType parseTargetType(String value) {
        if (value == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return ReportTargetType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private ReportReason parseReason(String value) {
        if (value == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return ReportReason.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // ---- 필터 파싱 (잘못되면 전체 = null, 400 안 던짐) ----

    private String normalizeTargetType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ReportTargetType.valueOf(value.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
