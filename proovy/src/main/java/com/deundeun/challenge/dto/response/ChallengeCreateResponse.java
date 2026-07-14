package com.deundeun.challenge.dto.response;

import com.deundeun.challenge.domain.Challenge;

public record ChallengeCreateResponse(Long id) {

    public static ChallengeCreateResponse from (Challenge challenge) {
        return new ChallengeCreateResponse(challenge.getId());
    }

}
