package com.deundeun.chat.dto.response;

import com.deundeun.certification.dto.chat.SharedCertificationInfo;

import java.time.LocalDateTime;

public record SharedCertificationResponse(
    Long certificationId,
    Long challengeId,
    String challengeTitle,
    Long authorId,
    String authorNickname,
    String thumbnailUrl,
    LocalDateTime certifiedAt
) {

    public static SharedCertificationResponse of(SharedCertificationInfo info) {
        return new SharedCertificationResponse(
            info.certificationId(),
            info.challengeId(),
            info.challengeTitle(),
            info.authorId(),
            info.authorNickname(),
            info.thumbnailUrl(),
            info.certifiedAt()
        );
    }
}
