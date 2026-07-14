package com.deundeun.certification.mapper;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.ChallengeForCertification;
import com.deundeun.certification.dto.CreateCertificationPostSqlParam;
import com.deundeun.certification.dto.ParticipantForCertification;
import com.deundeun.certification.dto.PendingCertificationResponse;
import com.deundeun.certification.dto.PostReviewContext;
import com.deundeun.certification.enums.ApprovalType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CertificationMapper {

   // 인증글 등록
   public Long createCertificationPost(CreateCertificationPostSqlParam param);

   // 챌린지 존재하는지, 상태어떤지
   ChallengeForCertification findChallengeById(Long challengeId);

   // 이 유저가 이 챌린지의 참가자인지 조회
   ParticipantForCertification findParticipant(@Param("challengeId") Long challengeId,
                                               @Param("userId") Long userId);

   // 오늘 참가자가 등록한 인증글 수 — 한챌린지당 하루 1개 제한
   int countTodayCertification(Long challengeParticipantId);

   // 추가 이미지 여러 장을 한 번에 저장
   void insertPostImages(@Param("postId") Long postId,
                         @Param("imageList") List<String> imageList);

   // 인증글 상세 1건 조회
   CertificationPostDetailResponse findPostDetail(Long postId);

   // 그 글의 추가 이미지 URL 목록 조회
   List<String> findPostImageUrls(Long postId);

   // 글 작성자 조회
   Long findPostAuthorId(Long postId);

   // 관리자인지 확인
   int isAdmin(Long userId);

   // 글 소프트 삭제
   void softDeletePost(Long postId);

   // 승인/반려용: 글 상태 + 방장
   PostReviewContext findPostReviewContext(Long postId);

   // 글 승인 (approvalType: MANUAL=방장수동 / AUTO=스케줄러자동). 반환=영향받은 행 수
   int approvePost(@Param("postId") Long postId,
                   @Param("approvalType") ApprovalType approvalType);

   // 글 반려. 반환=영향받은 행 수
   int rejectPost(@Param("postId") Long postId, @Param("reason") String reason);

   // 특정 챌린지의 승인대기 인증글 목록 (방장 검수용)
   List<PendingCertificationResponse> findPendingCertifications(Long challengeId);

   // 글 본문·대표이미지 수정 + PENDING 회귀
   void updatePost(@Param("postId") Long postId,
                   @Param("contents") String contents,
                   @Param("thumbnailImage") String thumbnailImage);

   // 그 글의 추가 이미지 전부 삭제 (수정 시 통째 교체용)
   void deletePostImages(Long postId);

}
