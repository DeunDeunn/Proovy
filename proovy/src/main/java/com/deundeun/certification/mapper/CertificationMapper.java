package com.deundeun.certification.mapper;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.ChallengeForCertification;
import com.deundeun.certification.dto.CreateCertificationPostSqlParam;
import com.deundeun.certification.dto.FeedItemResponse;
import com.deundeun.certification.dto.FeedQuery;
import com.deundeun.certification.dto.ParticipantForCertification;
import com.deundeun.certification.dto.ParticipantSuccessCount;
import com.deundeun.certification.dto.PendingCertificationResponse;
import com.deundeun.certification.dto.PendingPostForAutoApproval;
import com.deundeun.certification.dto.PostReviewContext;
import com.deundeun.certification.dto.chat.SharedCertificationInfo;
import com.deundeun.certification.dto.TodayCertificationProgressResponse;
import com.deundeun.certification.enums.ApprovalType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CertificationMapper {

   // 인증글 등록
   public Long createCertificationPost(CreateCertificationPostSqlParam param);

   // 챌린지 존재하는지, 상태어떤지
   ChallengeForCertification findChallengeById(Long challengeId);

   // 인증글이 속한 챌린지 조회 (수정 시 시간대 검증용 — 글→참가자→챌린지 조인)
   ChallengeForCertification findChallengeByPostId(Long postId);

   // 이 유저가 이 챌린지의 참가자인지 조회
   ParticipantForCertification findParticipant(@Param("challengeId") Long challengeId,
                                               @Param("userId") Long userId);

   // 오늘 참가자가 등록한 인증글 수 — 한챌린지당 하루 1개 제한
   int countTodayCertification(Long challengeParticipantId);

   // 홈의 오늘 인증 현황: 진행 중인 챌린지 수와 오늘 인증을 등록한 챌린지 수
   TodayCertificationProgressResponse findTodayCertificationProgress(Long userId);

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

   // 자정 자동 승인 대상: 삭제 안 된 PENDING 글 전체 (작성자·챌린지·방장 포함)
   List<PendingPostForAutoApproval> findAllPendingPostsForAutoApproval();

   // 자동 승인: id 목록을 한 번에 APPROVED/AUTO로 갱신. 반환=실제 갱신된 글 id 목록(RETURNING id)
   List<Long> approvePostsAuto(@Param("postIds") List<Long> postIds);

   // 참가자별 APPROVED 인증 일수 집계 (챌린지 도메인 성공판정 제공용). 인증 0건 참가자는 결과에 없음
   List<ParticipantSuccessCount> countApprovedDaysByParticipantIds(@Param("participantIds") List<Long> participantIds);

   // 한 참가자의 승인된 인증 날짜 목록 (중복 제거, 최신순). 삭제된 글도 성공 실적으로 포함한다.
   List<LocalDate> findApprovedCertificationDates(Long challengeParticipantId);

   // 특정 챌린지의 승인대기 인증글 목록 (방장 검수용, 커서 무한스크롤·오래된 순)
   List<PendingCertificationResponse> findPendingCertifications(@Param("challengeId") Long challengeId,
                                                                @Param("cursor") Long cursor,
                                                                @Param("size") int size);

   // 글 본문·대표이미지 수정 + PENDING 회귀
   void updatePost(@Param("postId") Long postId,
                   @Param("contents") String contents,
                   @Param("thumbnailImage") String thumbnailImage);

   // 그 글의 추가 이미지 전부 삭제 (수정 시 통째 교체용)
   void deletePostImages(Long postId);

   // 채팅 메시지의 인증글 공유 카드용 요약 정보 (챌린지 제목/작성자 닉네임 포함), 배치 조회
   List<SharedCertificationInfo> findSharedCertifications(@Param("postIds") List<Long> postIds);

   // 피드 목록 조회 (챌린지/전체/내/타인 피드 공통).
   List<FeedItemResponse> findFeed(FeedQuery query);

   // 좋아요 삭제, 0 or 1
   int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);

   // 좋아요 등록, 중복 X / 0 or 1
   int insertLike(@Param("postId") Long postId, @Param("userId") Long userId);

   // 좋아요 +1
   void incrementLikeCount(Long postId);

   // 좋아요 -1
   void decrementLikeCount(Long postId);

   // 해당글 좋아요집계
   long findLikeCount(Long postId);

   // 현재 사용자가 해당 글에 좋아요를 눌렀는지
   boolean existsLike(@Param("postId") Long postId, @Param("userId") Long userId);
}
