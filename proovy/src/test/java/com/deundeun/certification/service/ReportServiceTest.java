package com.deundeun.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.certification.dto.AdminReportListResponse;
import com.deundeun.certification.dto.CreateReportRequest;
import com.deundeun.certification.dto.CreateReportSqlParam;
import com.deundeun.certification.dto.ReportForProcess;
import com.deundeun.certification.enums.ReportStatus;
import com.deundeun.certification.enums.ReportTargetType;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.certification.mapper.ReportMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService - 신고 등록/조회/처리")
class ReportServiceTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private CertificationMapper certificationMapper;
    @Mock
    private CertificationService certificationService;

    @InjectMocks
    private ReportService reportService;

    private static final Long USER_ID = 10L;
    private static final Long ADMIN_ID = 1L;

    private CreateReportRequest req(String targetType, Long targetId, String reason, String detail) {
        CreateReportRequest r = new CreateReportRequest();
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReason(reason);
        r.setDetail(detail);
        return r;
    }

    private ReportForProcess pendingReport(ReportTargetType type, long targetId) {
        ReportForProcess r = new ReportForProcess();
        r.setId(50L);
        r.setStatus(ReportStatus.PENDING);
        r.setTargetType(type);
        r.setTargetId(targetId);
        return r;
    }

    @Nested
    @DisplayName("createReport")
    class Create {
        @Test
        @DisplayName("[R-01] targetType이 잘못되면 INVALID_REQUEST")
        void badTargetType() {
            assertThatThrownBy(() -> reportService.createReport(USER_ID, req("WRONG", 1L, "SPAM", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("[R-02] reason이 잘못되면 INVALID_REQUEST")
        void badReason() {
            assertThatThrownBy(() -> reportService.createReport(USER_ID, req("POST", 1L, "WRONG", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("[R-03] targetId가 null이면 INVALID_REQUEST")
        void nullTargetId() {
            assertThatThrownBy(() -> reportService.createReport(USER_ID, req("POST", null, "SPAM", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("[R-04] detail이 500자를 넘으면 INVALID_REQUEST")
        void detailTooLong() {
            String longDetail = "a".repeat(501);
            assertThatThrownBy(() -> reportService.createReport(USER_ID, req("POST", 1L, "SPAM", longDetail)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("[R-05] COMMENT 대상의 소속 글을 못 찾으면 REPORT_TARGET_NOT_FOUND")
        void commentTargetMissing() {
            when(reportMapper.findPostIdByCommentId(1L)).thenReturn(null);

            assertThatThrownBy(() -> reportService.createReport(USER_ID, req("COMMENT", 1L, "SPAM", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
        }

        @Test
        @DisplayName("[R-06] 이미 신고한 대상이면(insert 0행) ALREADY_REPORTED")
        void duplicate() {
            when(reportMapper.insertReport(any(CreateReportSqlParam.class))).thenReturn(0);

            assertThatThrownBy(() -> reportService.createReport(USER_ID, req("POST", 1L, "SPAM", null)))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_REPORTED);
            // 읽기 게이트를 통과했는지도 확인
            verify(certificationService).assertPostReadable(1L, USER_ID);
        }

        @Test
        @DisplayName("[R-07] 정상 신고(POST)면 insert 후 생성 id 반환")
        void success() {
            doAnswer(inv -> {
                CreateReportSqlParam p = inv.getArgument(0);
                p.setId(77L);
                return 1;
            }).when(reportMapper).insertReport(any(CreateReportSqlParam.class));

            Long id = reportService.createReport(USER_ID, req("post", 1L, "spam", "욕설 포함"));

            assertThat(id).isEqualTo(77L);
        }
    }

    @Nested
    @DisplayName("getReportsForAdmin")
    class ListForAdmin {
        @Test
        @DisplayName("[R-08] 관리자가 아니면 FORBIDDEN")
        void notAdmin() {
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> reportService.getReportsForAdmin(USER_ID, null, 0, 20))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[R-09] 관리자면 목록+총건수 반환, page/size 안전값 반영")
        void success() {
            when(certificationMapper.isAdmin(ADMIN_ID)).thenReturn(1);
            when(reportMapper.findReportsForAdmin(any())).thenReturn(List.of());
            when(reportMapper.countReportsForAdmin(any())).thenReturn(0L);

            AdminReportListResponse res = reportService.getReportsForAdmin(ADMIN_ID, "POST", null, null);

            assertThat(res.getPage()).isEqualTo(0);
            assertThat(res.getSize()).isEqualTo(20);   // size null → 기본 20
            assertThat(res.getTotalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("processReport")
    class Process {
        @Test
        @DisplayName("[R-10] 관리자가 아니면 FORBIDDEN")
        void notAdmin() {
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> reportService.processReport(USER_ID, 50L))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[R-11] 신고가 없으면 REPORT_NOT_FOUND")
        void notFound() {
            when(certificationMapper.isAdmin(ADMIN_ID)).thenReturn(1);
            when(reportMapper.findReportForProcess(50L)).thenReturn(null);

            assertThatThrownBy(() -> reportService.processReport(ADMIN_ID, 50L))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REPORT_NOT_FOUND);
        }

        @Test
        @DisplayName("[R-12] 이미 처리된 신고면 ALREADY_PROCESSED_REPORT")
        void alreadyProcessed() {
            when(certificationMapper.isAdmin(ADMIN_ID)).thenReturn(1);
            ReportForProcess r = pendingReport(ReportTargetType.POST, 1L);
            r.setStatus(ReportStatus.PROCESSED);
            when(reportMapper.findReportForProcess(50L)).thenReturn(r);

            assertThatThrownBy(() -> reportService.processReport(ADMIN_ID, 50L))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED_REPORT);
        }

        @Test
        @DisplayName("[R-13] 정상(POST) 처리 시 글 softDelete + 같은 대상 신고 일괄 처리")
        void success() {
            when(certificationMapper.isAdmin(ADMIN_ID)).thenReturn(1);
            when(reportMapper.findReportForProcess(50L)).thenReturn(pendingReport(ReportTargetType.POST, 7L));
            when(reportMapper.markProcessedByTarget("POST", 7L)).thenReturn(2);

            reportService.processReport(ADMIN_ID, 50L);

            verify(certificationMapper).softDeletePost(7L);
            verify(reportMapper).markProcessedByTarget("POST", 7L);
            verify(reportMapper, never()).softDeleteComment(anyLong());
        }
    }

    @Nested
    @DisplayName("rejectReport")
    class Reject {
        @Test
        @DisplayName("[R-14] 정상 기각 시 markRejected 호출")
        void success() {
            when(certificationMapper.isAdmin(ADMIN_ID)).thenReturn(1);
            when(reportMapper.findReportForProcess(50L)).thenReturn(pendingReport(ReportTargetType.POST, 7L));
            when(reportMapper.markRejected(50L)).thenReturn(1);

            reportService.rejectReport(ADMIN_ID, 50L);

            verify(reportMapper).markRejected(50L);
        }

        @Test
        @DisplayName("[R-15] 동시성으로 0행이면 ALREADY_PROCESSED_REPORT")
        void raceCondition() {
            when(certificationMapper.isAdmin(ADMIN_ID)).thenReturn(1);
            when(reportMapper.findReportForProcess(50L)).thenReturn(pendingReport(ReportTargetType.POST, 7L));
            when(reportMapper.markRejected(50L)).thenReturn(0);

            assertThatThrownBy(() -> reportService.rejectReport(ADMIN_ID, 50L))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED_REPORT);
        }
    }
}
