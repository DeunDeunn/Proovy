package com.deundeun.certification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 미검수 경고·페널티용 매퍼.
// ⚠️ 회원 도메인 테이블(user_warnings, users, user_verifications)을 갱신하는 지점을 여기 한곳에 모음.
@Mapper
public interface HostWarningMapper {

    List<Long> findPendingReviewHostIds();

    List<Long> insertWarningsForPendingChallenges(@Param("hostId") Long hostId);

    // 이 유저의 유효(ACTIVE) 경고 수 — 3회 누적 판정용
    int countActiveWarnings(Long userId);

    // 우수회원 여부: user_verifications 최신 건이 APPROVED면 우수회원
    boolean isExcellentMember(Long userId);

    // 강등 ①: 우수회원 인증을 REVOKED로 회수. 반환=영향받은 행 수
    int revokeVerification(Long userId);

    // 강등 ②: users.demoted_at 기록 (이후 우수회원 재신청 시 이 시점 이후 실적만 인정됨)
    void setDemotedAt(Long userId);

    // 일반회원 페널티: 챌린지 개설 금지 종료 시점(penalted_at) 설정
    void setPenaltyDate(@Param("userId") Long userId, @Param("until") LocalDateTime until);

    // 페널티 적용 후 쌓인 경고를 RESOLVED로 소진 (카운트 0부터 재시작)
    void resolveActiveWarnings(Long userId);
}
