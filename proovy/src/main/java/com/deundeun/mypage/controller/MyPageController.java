package com.deundeun.mypage.controller;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import com.deundeun.mypage.dto.response.MyPageResponse;
import com.deundeun.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/api/mypage")
    public ApiResponse<MyPageResponse> getMyPage() {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(myPageService.getMyPage(userId));
    }
}
