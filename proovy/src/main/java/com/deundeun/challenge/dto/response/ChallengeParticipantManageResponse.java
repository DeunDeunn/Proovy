package com.deundeun.challenge.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeParticipantManageResponse {

    private Long userId;
    private String nickname;
    private LocalDateTime joinedAt;
    private Integer approvedDays;  // 인증 승인된 일수 (진행률 = approvedDays / 챌린지 총 기간)
    private Integer pendingCount;  // 검수 대기 중인 인증글 수

}
