package com.deundeun.auth.controller;

import com.deundeun.auth.dto.response.OtherUserProfileResponse;
import com.deundeun.auth.service.UserProfileService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}")

    public ApiResponse<OtherUserProfileResponse> getProfile(@PathVariable Long userId) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(userProfileService.getProfile(viewerId, userId));
    }
}
