package com.deundeun.certification.controller;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.CreateCertificationPostRequest;
import com.deundeun.certification.dto.CreateCertificationPostResponse;
import com.deundeun.certification.dto.PendingCertificationResponse;
import com.deundeun.certification.dto.RejectCertificationPostRequest;
import com.deundeun.certification.dto.UpdateCertificationPostRequest;
import com.deundeun.certification.service.CertificationService;
import com.deundeun.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class CertificationController {
    // TODO: userId는 테스트용 임시 (로그인 완성되면 교체)

    private final CertificationService certificationService;

    // 인증글 등록 API
    @PostMapping("/api/v1/challenge/{challengeId}/certification-post")
    public ApiResponse<CreateCertificationPostResponse> createCertificationPosts(
            @PathVariable Long challengeId,
            @RequestParam Long userId,
            @RequestBody CreateCertificationPostRequest request) {

        Long postId = certificationService.createCertificationPost(challengeId, userId, request);

        return ApiResponse.success(new CreateCertificationPostResponse(postId));
    }

    // 인증글 상세 조회 API
    @GetMapping("/api/v1/certification-post/{postId}")
    public ApiResponse<CertificationPostDetailResponse> getCertificationPostDetail(
            @PathVariable Long postId) {
        return ApiResponse.success(certificationService.getCertificationPostDetail(postId));
    }

    // 인증글 삭제 API
    @DeleteMapping("/api/v1/certification-post/{postId}")
    public ApiResponse<Void> deleteCertificationPost(@PathVariable Long postId,
                                                     @RequestParam Long userId) {
        certificationService.deleteCertificationPost(postId, userId);
        return ApiResponse.success(null);
    }

    // 인증글 승인 API
    @PatchMapping("/api/v1/certification-post/{postId}/approve")
    public ApiResponse<Void> approveCertificationPost(@PathVariable Long postId,
                                                      @RequestParam Long userId) {
        certificationService.approveCertificationPost(postId, userId);
        return ApiResponse.success(null);
    }

    // 인증글 반려 API
    @PatchMapping("/api/v1/certification-post/{postId}/reject")
    public ApiResponse<Void> rejectCertificationPost(@PathVariable Long postId,
                                                     @RequestParam Long userId,
                                                     @RequestBody RejectCertificationPostRequest request) {
        certificationService.rejectCertificationPost(postId, userId, request);
        return ApiResponse.success(null);
    }

    // 방장 검수 대기 목록 API
    @GetMapping("/api/v1/challenge/{challengeId}/pending-certifications")
    public ApiResponse<List<PendingCertificationResponse>> getPendingCertifications(
            @PathVariable Long challengeId,
            @RequestParam Long userId) {
        return ApiResponse.success(certificationService.getPendingCertifications(challengeId, userId));
    }

    // 인증글 수정 API
    @PutMapping("/api/v1/certification-post/{postId}")
    public ApiResponse<Void> updateCertificationPost(@PathVariable Long postId,
                                                     @RequestParam Long userId,
                                                     @RequestBody UpdateCertificationPostRequest request) {
        certificationService.updateCertificationPost(postId, userId, request);
        return ApiResponse.success(null);
    }
}
