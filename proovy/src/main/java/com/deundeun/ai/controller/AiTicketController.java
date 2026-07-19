package com.deundeun.ai.controller;

import com.deundeun.ai.dto.AiTicketPlanResponse;
import com.deundeun.ai.dto.AiTicketPurchaseRequest;
import com.deundeun.ai.dto.AiTicketPurchaseResponse;
import com.deundeun.ai.service.AiTicketService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-tickets")
public class AiTicketController {

    private final AiTicketService aiTicketService;

    @GetMapping("/plans")
    public ApiResponse<List<AiTicketPlanResponse>> findActivePlans() {
        return ApiResponse.success(aiTicketService.findActivePlans());
    }

    @PostMapping("/purchases")
    public ApiResponse<AiTicketPurchaseResponse> purchase(
            @RequestBody AiTicketPurchaseRequest request
    ) {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(aiTicketService.purchase(userId, request));
    }
}
