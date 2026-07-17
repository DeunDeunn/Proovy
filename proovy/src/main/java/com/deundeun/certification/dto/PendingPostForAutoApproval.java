package com.deundeun.certification.dto;

import lombok.Data;

// [조회 결과] 자정 자동 승인 대상 PENDING 글 1건 (경고·알림 처리에 필요한 정보 포함)
@Data
public class PendingPostForAutoApproval {
    private Long postId;
    private Long authorId;     // 글 작성자 (승인 알림 대상)
    private Long challengeId;  // 경고 기록용 (챌린지당 1건)
    private Long hostId;       // 챌린지 방장 (경고 대상)
}
