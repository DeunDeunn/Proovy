package com.deundeun.certification.dto;

import lombok.Data;
import java.time.LocalDateTime;
import com.deundeun.certification.enums.CertificationStatus;

// 피드 목록의 항목 1개- 피드 관련 api 4개 다 이거 씀
@Data
public class FeedItemResponse {
    private Long postId;
    private Long authorId;
    private String authorNickname;
    private String authorProfileImageUrl;
    private boolean authorBadgeApproved;
    private Long challengeId;
    private String thumbnailUrl;     // 대표인증이미지 URL
    private String contents;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private CertificationStatus status;
    private String rejectionReason;
}
