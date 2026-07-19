package com.deundeun.certification.dto;

import lombok.Data;
import java.time.LocalDateTime;

// 검수 대기 목록의 항목 1개 (커서 무한스크롤). 필드 규격은 FeedItemResponse와 동일하되 클래스는 분리.
@Data
public class PendingCertificationResponse {
    private Long postId;
    private Long authorId;
    private String authorNickname;
    private Long challengeId;
    private String thumbnailUrl;     // 대표인증이미지 URL
    private String contents;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime createdAt;
}
