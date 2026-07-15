package com.deundeun.challenge.controller;

import com.deundeun.challenge.dto.request.ChallengeCreateRequest;
import com.deundeun.challenge.dto.response.ChallengeCreateResponse;
import com.deundeun.challenge.service.ChallengeService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping
    public ApiResponse<ChallengeCreateResponse> createChallenge(
            @Valid @RequestBody ChallengeCreateRequest request) {
        Long hostId = CurrentUser.getUserId();
        return ApiResponse.success(challengeService.create(hostId, request));
    }


}
