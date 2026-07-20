package com.deundeun.challenge.dto.response;

import com.deundeun.challenge.domain.ParticipantStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeParticipantListItemResponse {

    private Long userId;
    private String nickname;
    private ParticipantStatus status;
    private LocalDateTime joinedAt;

}
