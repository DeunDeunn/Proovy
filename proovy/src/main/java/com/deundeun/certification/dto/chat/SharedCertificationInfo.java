package com.deundeun.certification.dto.chat;

import java.time.LocalDateTime;

// 채팅 메시지에서 인증글을 공유할 때 필요한 요약 정보
public record SharedCertificationInfo(
    Long certificationId,
    Long challengeId,
    String challengeTitle,
    Long authorId,
    String authorNickname,
    String thumbnailUrl,
    LocalDateTime certifiedAt
) {
}
