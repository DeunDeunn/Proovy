package com.deundeun.certification.controller;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.CreateCertificationPostRequest;
import com.deundeun.certification.dto.FeedItemResponse;
import com.deundeun.certification.dto.LikeToggleResponse;
import com.deundeun.certification.dto.CreateCertificationPostResponse;
import com.deundeun.certification.dto.PendingCertificationResponse;
import com.deundeun.certification.dto.RejectCertificationPostRequest;
import com.deundeun.certification.dto.UpdateCertificationPostRequest;
import com.deundeun.certification.service.CertificationService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class CertificationController {

    private final CertificationService certificationService;

    // 인증글 등록 API (멀티파트: JSON 본문 + 대표이미지 파일 + 추가이미지 파일들)
    @PostMapping(value = "/api/v1/challenge/{challengeId}/certification-post",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreateCertificationPostResponse> createCertificationPosts(
            @PathVariable Long challengeId,
            @RequestPart("request") CreateCertificationPostRequest request,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        Long userId = CurrentUser.getUserId();
        Long postId = certificationService.createCertificationPost(challengeId, userId, request, thumbnail, images);

        return ApiResponse.success(new CreateCertificationPostResponse(postId));
    }

    // 인증글 상세 조회 API
    @GetMapping("/api/v1/certification-post/{postId}")
    public ApiResponse<CertificationPostDetailResponse> getCertificationPostDetail(
            @PathVariable Long postId) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(certificationService.getCertificationPostDetail(postId, viewerId));
    }

    // 인증글 삭제 API
    @DeleteMapping("/api/v1/certification-post/{postId}")
    public ApiResponse<Void> deleteCertificationPost(@PathVariable Long postId) {
        Long userId = CurrentUser.getUserId();
        certificationService.deleteCertificationPost(postId, userId);
        return ApiResponse.success(null);
    }

    // 인증글 승인 API
    @PatchMapping("/api/v1/certification-post/{postId}/approve")
    public ApiResponse<Void> approveCertificationPost(@PathVariable Long postId) {
        Long userId = CurrentUser.getUserId();
        certificationService.approveCertificationPost(postId, userId);
        return ApiResponse.success(null);
    }

    // 인증글 반려 API
    @PatchMapping("/api/v1/certification-post/{postId}/reject")
    public ApiResponse<Void> rejectCertificationPost(@PathVariable Long postId,
                                                     @RequestBody RejectCertificationPostRequest request) {
        Long userId = CurrentUser.getUserId();
        certificationService.rejectCertificationPost(postId, userId, request);
        return ApiResponse.success(null);
    }

    // 방장 검수 대기 목록 API (커서 무한스크롤·오래된 순)
    @GetMapping("/api/v1/challenge/{challengeId}/pending-certifications")
    public ApiResponse<List<PendingCertificationResponse>> getPendingCertifications(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(
                certificationService.getPendingCertifications(challengeId, userId, cursor, size));
    }

    // 인증글 수정 API (멀티파트: JSON 본문 + 새 대표이미지 파일 + 새 추가이미지 파일들)
    @PutMapping(value = "/api/v1/certification-post/{postId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateCertificationPost(
            @PathVariable Long postId,
            @RequestPart("request") UpdateCertificationPostRequest request,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        Long userId = CurrentUser.getUserId();
        certificationService.updateCertificationPost(postId, userId, request, thumbnail, images);
        return ApiResponse.success(null);
    }

    // #1 챌린지 피드 /챌린지 참가자만 볼수있음
    @GetMapping("/api/v1/challenge/{challengeId}/feed")
    public ApiResponse<List<FeedItemResponse>> getChallengeFeed(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long cursorLike,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String sort) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(
                certificationService.getChallengeFeed(challengeId, viewerId, cursor, cursorLike, size, filter, sort));
    }

    // #2 전체 피드 / 로그인 한 누구나 공개
    @GetMapping("/api/v1/feed")
    public ApiResponse<List<FeedItemResponse>> getPublicFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long cursorLike,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String sort) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(
                certificationService.getPublicFeed(viewerId, cursor, cursorLike, size, filter, sort));
    }

    // #3 내 피드 (내 글 전부, 상태 무관, 필터 없음)
    @GetMapping("/api/v1/me/certification-posts")
    public ApiResponse<List<FeedItemResponse>> getMyFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(
                certificationService.getMyFeed(viewerId, cursor, size));
    }

    // #4 타인 피드 (그 사람 APPROVED, 비공개 챌린지 글은 뷰어가 참가자일 때만, 필터 없음)
    @GetMapping("/api/v1/users/{userId}/certification-posts")
    public ApiResponse<List<FeedItemResponse>> getUserFeed(
            @PathVariable Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(
                certificationService.getUserFeed(userId, viewerId, cursor, size));
    }

    // 좋아요 토글 우저가 읽을 수 있는 ㅅ승인글에만). 누르면 등록, 다시 누르면 취소.
    @PostMapping("/api/v1/certification-post/{postId}/like")
    public ApiResponse<LikeToggleResponse> toggleLike(@PathVariable Long postId) {
        Long viewerId = CurrentUser.getUserId();
        return ApiResponse.success(certificationService.toggleLike(postId, viewerId));
    }
}
