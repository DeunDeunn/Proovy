package com.deundeun.certification.controller;

import com.deundeun.certification.dto.AdminReportListResponse;
import com.deundeun.certification.dto.CreateReportRequest;
import com.deundeun.certification.dto.CreateReportResponse;
import com.deundeun.certification.service.ReportService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class ReportController {

    private final ReportService reportService;

    // 신고 등록 API (게시글/댓글)
    @PostMapping("/api/v1/reports")
    public ApiResponse<CreateReportResponse> createReport(
            @RequestBody CreateReportRequest request) {

        Long userId = CurrentUser.getUserId();
        Long reportId = reportService.createReport(userId, request);

        return ApiResponse.success(new CreateReportResponse(reportId));
    }

    // 관리자 신고 목록 조회 API (PENDING만 노출)
    @GetMapping("/api/v1/admin/reports")
    public ApiResponse<AdminReportListResponse> getReportsForAdmin(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(
                reportService.getReportsForAdmin(userId, targetType, page, size));
    }

    // 관리자 신고 처리 API (PENDING → PROCESSED, 신고 인정)
    @PatchMapping("/api/v1/admin/reports/{reportId}/process")
    public ApiResponse<Void> processReport(@PathVariable Long reportId) {
        Long userId = CurrentUser.getUserId();
        reportService.processReport(userId, reportId);
        return ApiResponse.success(null);
    }

    // 관리자 신고 기각 API (PENDING → REJECTED, 신고 무효)
    @PatchMapping("/api/v1/admin/reports/{reportId}/reject")
    public ApiResponse<Void> rejectReport(@PathVariable Long reportId) {
        Long userId = CurrentUser.getUserId();
        reportService.rejectReport(userId, reportId);
        return ApiResponse.success(null);
    }
}
