package com.deundeun.challenge.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeParticipant {

    private Long id;
    private Long challengeId;
    private Long userId; // users.id (도메인 밖, FK 없음)
    private ParticipantStatus status;
    private ParticipantResult result;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
