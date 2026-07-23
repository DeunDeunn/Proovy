package com.deundeun.certification.dto;

import com.deundeun.certification.enums.CertificationStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

// 인증글 상세 조회 결과
@Data
public class CertificationPostDetailResponse {
    private Long postId;
    private Long authorId;          // 작성자아이디
    private String authorNickname;
    private String authorProfileImageUrl;
    private boolean authorBadgeApproved; // 최신 회원 인증 상태가 APPROVED인지 여부
    private String contents;
    private String thumbnailUrl;    // 대표 이미지 URL
    private CertificationStatus status;   // 방장 검수 상태
    private String approvalType;     // MANUAL / AUTO
    private String rejectionReason; // 반려 사유 (REJECTED일 때만 존재)
    private boolean aiReviewExpected;
    private CertificationAiReviewResponse aiReview;
    private boolean liked;          // 현재 로그인 사용자의 좋아요 여부
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private List<String> imageUrls; // 추가 이미지들 (별도 조회로 채움)
}
