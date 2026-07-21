package com.deundeun.challenge.dto.response;

import com.deundeun.challenge.domain.ChallengeParticipant;

import java.time.LocalDateTime;

public record ChallengeParticipantResponse(Long id, String status, LocalDateTime joinedAt) {

    public static ChallengeParticipantResponse from(ChallengeParticipant participant) {
        return new ChallengeParticipantResponse(
                participant.getId(),
                participant.getStatus().name(),
                participant.getJoinedAt());
    }
}
