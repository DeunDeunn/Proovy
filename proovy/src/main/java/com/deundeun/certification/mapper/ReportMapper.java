package com.deundeun.certification.mapper;

import com.deundeun.certification.dto.AdminReportItem;
import com.deundeun.certification.dto.AdminReportQuery;
import com.deundeun.certification.dto.CreateReportSqlParam;
import com.deundeun.certification.dto.ReportForProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    // 신고 대상 인증글이 존재하는지 (soft delete 제외). 존재하면 postId 반환, 없으면 null
    Long findPostIdIfExists(Long postId);

    // 신고 대상 댓글이 소속된 인증글 id 조회 (soft delete 제외). 없으면 null
    Long findPostIdByCommentId(Long commentId);

    // 신고 등록. UNIQUE(target_type, target_id, user_id) 충돌 시 아무것도 안 함 → 반환 0
    int insertReport(CreateReportSqlParam param);

    // ===== 관리자 =====

    // 관리자 신고 목록 (offset 페이징 + 필터)
    List<AdminReportItem> findReportsForAdmin(AdminReportQuery query);

    // 관리자 신고 목록 총 건수 (같은 필터 기준)
    long countReportsForAdmin(AdminReportQuery query);

    // 처리 대상 신고 1건 조회 (상태 + 삭제 대상 식별용). 없으면 null
    ReportForProcess findReportForProcess(Long reportId);

    // 같은 대상의 PENDING 신고를 모두 PROCESSED로 전이 (중복 신고 일괄 처리) + processed_at 기록. 갱신된 행 수 반환
    int markProcessedByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    // PENDING 신고를 REJECTED(기각)로 전이 + processed_at 기록. PENDING일 때만 갱신 → 0이면 이미 처리됨
    int markRejected(Long reportId);

    // 신고 대상 댓글 soft delete (deleted_at). 인증글은 CertificationMapper.softDeletePost 재사용
    void softDeleteComment(Long commentId);
}
