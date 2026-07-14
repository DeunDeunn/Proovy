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
    private String contents;
    private String thumbnailUrl;    // 대표 이미지 URL
    private CertificationStatus status;   // PENDING / APPROVED / REJECTED
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private List<String> imageUrls; // 추가 이미지들 (별도 조회로 채움)
}
