package com.deundeun.certification.service;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.ChallengeForCertification;
import com.deundeun.certification.dto.CreateCertificationPostRequest;
import com.deundeun.certification.dto.CreateCertificationPostSqlParam;
import com.deundeun.certification.dto.FeedItemResponse;
import com.deundeun.certification.dto.FeedQuery;
import com.deundeun.certification.dto.LikeToggleResponse;
import com.deundeun.certification.dto.ParticipantForCertification;
import com.deundeun.certification.dto.ParticipantSuccessCount;
import com.deundeun.certification.dto.PendingCertificationResponse;
import com.deundeun.certification.dto.PostReviewContext;
import com.deundeun.certification.dto.RejectCertificationPostRequest;
import com.deundeun.certification.dto.UpdateCertificationPostRequest;
import com.deundeun.certification.enums.ApprovalType;
import com.deundeun.certification.enums.CertificationStatus;
import com.deundeun.certification.enums.FeedFilter;
import com.deundeun.certification.enums.FeedSort;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.file.FileCategory;
import com.deundeun.global.file.TransactionalFileUploader;
import com.deundeun.notification.event.VerificationApprovedEvent;
import com.deundeun.notification.event.VerificationRejectedEvent;
import com.deundeun.notification.event.VerificationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 인증글 도메인의 규칙·판단 담당(Service).
 * 역할: 등록/검증 규칙을 순서대로 확인하고, Mapper(SQL)를 불러 조립함.
 * (Controller와 DB 사이의 '두뇌' 역할)
 */
@RequiredArgsConstructor
@Service
public class CertificationService {

    private final CertificationMapper certificationMapper;
    private final ApplicationEventPublisher eventPublisher;   // 알림 이벤트
    private final TransactionalFileUploader transactionalFileUploader;   // S3 업로드 + 롤백 시 자동삭제
    /*인증글 등록
    // TODO: Challenge 테이블을 셀렉트 하여, 상태와 인증 게시 가능 시간을 가져와 확인 (셀렉트)
    //  CertificationMapper에 Challenge를 셀렉트 하는 쿼리를 작성한 뒤, 서비스단에서 호출.
    //  결과의 상태와 게시가능 시간을 확인
    //  해당 유저가 ChallengeParticipant 테이블에 존재하며, 강퇴당하거나 중도에 나가지 않았는지 체크 (셀렉트)
    //  CertificationMapper에 ChallengeParticipant를 셀렉트 하는 쿼리를 작성한 뒤, 서비스단에서 호출
    //  결과가 있으면 작성 가능. 없으면 작성할 수 없음.
    //*/
    @Transactional
    public Long createCertificationPost(Long challengeId, Long userId, CreateCertificationPostRequest request,
                                        MultipartFile thumbnail, List<MultipartFile> images){

        // 대표 이미지 필수 (파일 업로드 전에 먼저 걸러 S3 낭비 방지)
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new ApiException(ErrorCode.THUMBNAIL_REQUIRED);
        }
        // 추가 이미지 최대 3장
        if (images != null && images.size() > 3) {
            throw new ApiException(ErrorCode.TOO_MANY_IMAGES);
        }

        // 챌린지 확인-존재하고 진행중인지
        ChallengeForCertification challenge = certificationMapper.findChallengeById(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        if (!"IN_PROGRESS".equals(challenge.getStatus())) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_IN_PROGRESS);
        }

        // 인증 등록 가능 시간대(KST)인지 검증
        validateCertTimeRange(challenge);

        // 참가자 확인
        ParticipantForCertification participant = certificationMapper.findParticipant(challengeId, userId);
        if (participant == null) {
            throw new ApiException(ErrorCode.NOT_CHALLENGE_PARTICIPANT);
        }
        if (!"ACTIVE".equals(participant.getStatus())) {
            throw new ApiException(ErrorCode.PARTICIPANT_NOT_ACTIVE);
        }

        // 한 챌린지당 하루 1개 인증글 제한
        int todayCount = certificationMapper.countTodayCertification(participant.getId());
        if (todayCount > 0) {
            throw new ApiException(ErrorCode.ALREADY_CERTIFIED_TODAY);
        }

        // 대표 이미지 S3 업로드 (트랜잭션 롤백 시 자동 삭제 예약됨)
        String thumbnailUrl = transactionalFileUploader.upload(thumbnail, FileCategory.CERTIFICATION);
        // 추가 이미지 업로드 (없으면 빈 리스트). uploadAll은 하나라도 실패하면 전부 정리
        List<String> imageUrls = (images == null || images.isEmpty())
                ? List.of()
                : transactionalFileUploader.uploadAll(images, FileCategory.CERTIFICATION);

        CreateCertificationPostSqlParam param = new CreateCertificationPostSqlParam(
                null,
                userId,
                participant.getId(),
                request.getContents(),
                thumbnailUrl
        );
        certificationMapper.createCertificationPost(param);

        // 추가 이미지가 있으면 저장 (없으면 건너뜀 )
        if (!imageUrls.isEmpty()) {
            certificationMapper.insertPostImages(param.getId(), imageUrls);
        }

        // 방장에게 인증 게시글 검수 알림
        eventPublisher.publishEvent(new VerificationSubmittedEvent(challenge.getHostId(), param.getId()));

        return param.getId();
    }

    // 인증글 상세 조회 (읽기 권한 게이트 적용)
    public CertificationPostDetailResponse getCertificationPostDetail(Long postId, Long viewerId) {

        // 글 1건 조회 (삭제글은 findPostDetail이 이미 제외 → null이면 없음)
        CertificationPostDetailResponse detail = certificationMapper.findPostDetail(postId);
        if (detail == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        // 읽기 권한 게이트 (작성자·관리자·방장·공개범위)
        assertReadable(postId, viewerId, detail);

        // 추가 이미지
        List<String> imageUrls = certificationMapper.findPostImageUrls(postId);
        detail.setImageUrls(imageUrls);

        return detail;
    }

    /**
     * 신고 등 다른 서비스에서 재사용: 이 글이 뷰어에게 읽기 가능한지 검사.
     * 읽을 수 없으면(없거나 권한 없음) POST_NOT_FOUND로 존재를 숨긴다.
     */
    public void assertPostReadable(Long postId, Long viewerId) {
        CertificationPostDetailResponse detail = certificationMapper.findPostDetail(postId);
        if (detail == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        assertReadable(postId, viewerId, detail);
    }

    /**
     * 권한 게이트: 뷰어가 이 글을 읽을 수 있는지 검사
     * 규칙: 작성자 본인 / 관리자 / 그 챌린지 방장 → 상태 무관. 그 외 → APPROVED + 공개범위.
     */
    private void assertReadable(Long postId, Long viewerId, CertificationPostDetailResponse detail) {
        boolean isAuthor = detail.getAuthorId().equals(viewerId);
        boolean isAdmin = certificationMapper.isAdmin(viewerId) > 0;
        if (isAuthor || isAdmin) {
            return;
        }
        // 챌린지 컨텍스트 조회 (방장 판별 + 공개범위 확인)
        ChallengeForCertification challenge = certificationMapper.findChallengeByPostId(postId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        // 그 챌린지 방장이면 자기 챌린지 글은 상태 무관 열람 가능 (검수 목적)
        if (challenge.getHostId().equals(viewerId)) {
            return;
        }
        // 그 외: 승인된 글만 (미승인글은 존재 숨김)
        if (detail.getStatus() != CertificationStatus.APPROVED) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        // 공개범위: PARTICIPANTS_ONLY면 그 챌린지 ACTIVE 참가자만
        if ("PARTICIPANTS_ONLY".equals(challenge.getFeedVisibility())) {
            ParticipantForCertification participant =
                    certificationMapper.findParticipant(challenge.getId(), viewerId);
            if (participant == null || !"ACTIVE".equals(participant.getStatus())) {
                throw new ApiException(ErrorCode.POST_NOT_FOUND);
            }
        }
    }

    // 좋아요 토글 (읽을 수 있는 APPROVED 글에만). 누르면 등록, 다시 누르면 취소.
    @Transactional
    public LikeToggleResponse toggleLike(Long postId, Long viewerId) {
        // 글 존재 + 읽기 권한
        CertificationPostDetailResponse detail = certificationMapper.findPostDetail(postId);
        if (detail == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        assertReadable(postId, viewerId, detail);

        // 좋아요는 승인된 글에만 (작성자·방장·관리자가 미승인 글에 시도하는 경우 차단)
        if (detail.getStatus() != CertificationStatus.APPROVED) {
            throw new ApiException(ErrorCode.CANNOT_LIKE_UNAPPROVED);
        }

        // 토글:삭제 시도 → 지워졌으면 취소, 아니면 삽입 → 등록
        boolean liked;
        int deleted = certificationMapper.deleteLike(postId, viewerId);
        if (deleted > 0) {
            certificationMapper.decrementLikeCount(postId);
            liked = false;
        } else {
            int inserted = certificationMapper.insertLike(postId, viewerId);
            // 동시 요청으로 이미 삽입돼 있으면 inserted=0
            if (inserted > 0) {
                certificationMapper.incrementLikeCount(postId);
            }
            liked = true;
        }

        long likeCount = certificationMapper.findLikeCount(postId);
        return new LikeToggleResponse(liked, likeCount);
    }

    // 인증글 삭제
    public void deleteCertificationPost(Long postId, Long userId) {
        Long authorId = certificationMapper.findPostAuthorId(postId);
        if (authorId == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        // 권한: 작성자, 관리자
        boolean isAuthor = authorId.equals(userId);
        boolean isAdmin = certificationMapper.isAdmin(userId) > 0;
        if (!isAuthor && !isAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        certificationMapper.softDeletePost(postId);
    }

    // 인증글 승인 (방장 또는 관리자만, PENDING 글만)
    @Transactional
    public void approveCertificationPost(Long postId, Long userId) {
        PostReviewContext ctx = certificationMapper.findPostReviewContext(postId);
        if (ctx == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        // 권한: 방장 ,관리자
        boolean isHost = ctx.getHostId().equals(userId);
        boolean isAdmin = certificationMapper.isAdmin(userId) > 0;
        if (!isHost && !isAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        // 상태: PENDING만 승인 가능
        if (ctx.getStatus() != CertificationStatus.PENDING) {
            throw new ApiException(ErrorCode.NOT_PENDING_POST);
        }
        // 동시 요청 방어: PENDING일 때만 갱신되므로, 0행이면 이미 처리된 것 → 차단(이벤트도 발행 안 함)
        int updated = certificationMapper.approvePost(postId, ApprovalType.MANUAL);
        if (updated == 0) {
            throw new ApiException(ErrorCode.NOT_PENDING_POST);
        }
        // 알림
        eventPublisher.publishEvent(new VerificationApprovedEvent(ctx.getAuthorId(), postId));
    }

    // 인증글 반려 (방장/관리자만, PENDING 글만, 사유 필수)
    @Transactional
    public void rejectCertificationPost(Long postId, Long userId, RejectCertificationPostRequest request) {
        // 사유 필수
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ApiException(ErrorCode.REJECTION_REASON_REQUIRED);
        }
        PostReviewContext ctx = certificationMapper.findPostReviewContext(postId);
        if (ctx == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        // 권한: 방장,관리자
        boolean isHost = ctx.getHostId().equals(userId);
        boolean isAdmin = certificationMapper.isAdmin(userId) > 0;
        if (!isHost && !isAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        // 상태: PENDING만 반려 가능
        if (ctx.getStatus() != CertificationStatus.PENDING) {
            throw new ApiException(ErrorCode.NOT_PENDING_POST);
        }
        //코드래빗 피드백
        // 동시 요청 방어: PENDING일 때만 갱신되므로, 0행이면 이미 처리된 것 → 차단(이벤트도 발행 안 함)
        int updated = certificationMapper.rejectPost(postId, request.getReason());
        if (updated == 0) {
            throw new ApiException(ErrorCode.NOT_PENDING_POST);
        }
        // 작성자에게 반려됐다고 알림
        eventPublisher.publishEvent(new VerificationRejectedEvent(ctx.getAuthorId(), postId));
        // TODO: 반려글 24시간 후 자동삭제 (스케줄러 — 별도)
    }

    // 검수 대기 목록 조회 (그 챌린지 방장 또는 관리자만, 커서 무한스크롤·최신순)
    public List<PendingCertificationResponse> getPendingCertifications(Long challengeId, Long userId,
                                                                       Long cursor, Integer size) {
        ChallengeForCertification challenge = certificationMapper.findChallengeById(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        // 권한: 방장 또는 관리자
        boolean isHost = challenge.getHostId().equals(userId);
        boolean isAdmin = certificationMapper.isAdmin(userId) > 0;
        if (!isHost && !isAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return certificationMapper.findPendingCertifications(challengeId, cursor, clampSize(size));
    }

    // 인증글 수정 (수정하면 PENDING 회귀)
    @Transactional
    public void updateCertificationPost(Long postId, Long userId, UpdateCertificationPostRequest request,
                                        MultipartFile thumbnail, List<MultipartFile> images) {
        // 대표 이미지 필수 + 최대 3장 (업로드 전에 먼저 걸러냄)
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new ApiException(ErrorCode.THUMBNAIL_REQUIRED);
        }
        if (images != null && images.size() > 3) {
            throw new ApiException(ErrorCode.TOO_MANY_IMAGES);
        }

        // 글 조회(작성자 확인 + 옛 썸네일 URL 확보). 삭제글은 findPostDetail이 제외 → null이면 없음
        CertificationPostDetailResponse detail = certificationMapper.findPostDetail(postId);
        if (detail == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        if (!detail.getAuthorId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);   // 작성자 본인만 수정 가능
        }

        // 수정도 인증 등록 가능 시간대 안에서만 가능
        //코드래빗 피드백
        // challenge_participant_id에 FK가 없어 연관이 깨진 글이 들어올 수 있음.
        // 조인 결과가 null이면 검증을 건너뛰지 말고 실패 처리(검증 우회 방지).
        ChallengeForCertification challenge = certificationMapper.findChallengeByPostId(postId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        validateCertTimeRange(challenge);

        // 새 대표이미지 업로드: 커밋되면 옛 썸네일 삭제, 롤백되면 방금 올린 새 파일 삭제
        String thumbnailUrl = transactionalFileUploader.uploadReplacing(
                thumbnail, FileCategory.CERTIFICATION, detail.getThumbnailUrl());
        // 새 추가이미지 업로드(롤백 시 자동삭제).
        // ※ 옛 추가이미지 S3 파일은 지금은 정리하지 않음(MVP) — 고아 파일은 추후 정합성 배치로 정리.
        List<String> imageUrls = (images == null || images.isEmpty())
                ? List.of()
                : transactionalFileUploader.uploadAll(images, FileCategory.CERTIFICATION);

        // 본문·대표이미지 수정 + PENDING 회귀
        certificationMapper.updatePost(postId, request.getContents(), thumbnailUrl);

        // 추가 이미지 기존 삭제 → 새 목록 재삽입 (DB만; 옛 S3 파일은 위 주석대로 미정리)
        certificationMapper.deletePostImages(postId);
        if (!imageUrls.isEmpty()) {
            certificationMapper.insertPostImages(postId, imageUrls);
        }

    }

    // 인증 등록 가능 시간검증. 현재 시각이 certStartTime, certEndTime 범위 밖이면 예외.
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private void validateCertTimeRange(ChallengeForCertification challenge) {
        LocalTime now = LocalTime.now(KST);
        LocalTime start = challenge.getCertStartTime();
        LocalTime end = challenge.getCertEndTime();

        if (start == null || end == null) {
            return;
        }
        if (now.isBefore(start) || now.isAfter(end)) {
            throw new ApiException(ErrorCode.NOT_IN_CERT_TIME_RANGE);
        }
    }

    // ── 피드 조회 ──

    //TODO 화면 생기면 보기
    // size 클램프 (기본 20, 최대 50). null/이상값이면 기본값.
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private int clampSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    // 인기순 커서는 (cursor, cursorLike) 복합키 → 부분 입력 거부 (하나만 오면 첫 페이지 반복 버그)
    private void validatePopularCursor(FeedSort sort, Long cursor, Long cursorLike) {
        if (sort == FeedSort.POPULAR && (cursor == null) != (cursorLike == null)) {
            throw new ApiException(ErrorCode.INVALID_POPULAR_CURSOR);
        }
    }

    //챌린지 피드 — 그 챌린지 참가자랑 승인글만
    public List<FeedItemResponse> getChallengeFeed(Long challengeId, Long viewerId,
                                                   Long cursor, Long cursorLike, Integer size,
                                                   String filter, String sort) {
        // 챌린지 존재 확인
        ChallengeForCertification challenge = certificationMapper.findChallengeById(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        // 접근하려면 그 챌린지 참가자 활성 상태여야함
        ParticipantForCertification participant = certificationMapper.findParticipant(challengeId, viewerId);
        if (participant == null) {
            throw new ApiException(ErrorCode.NOT_CHALLENGE_PARTICIPANT);
        }
        if (!"ACTIVE".equals(participant.getStatus())) {
            throw new ApiException(ErrorCode.PARTICIPANT_NOT_ACTIVE);
        }

        FeedSort feedSort = FeedSort.from(sort);
        validatePopularCursor(feedSort, cursor, cursorLike);

        FeedQuery q = new FeedQuery();
        q.setChallengeId(challengeId);
        q.setViewerId(viewerId);
        q.setFilter(FeedFilter.from(filter));
        q.setSort(feedSort);
        q.setCursor(cursor);
        q.setCursorLike(cursorLike);   // 인기순 커서 복합키
        q.setSize(clampSize(size));
        return certificationMapper.findFeed(q);
    }

    // 전체 피드 — 로그인 누구나, 전체공개 챌린지
    public List<FeedItemResponse> getPublicFeed(Long viewerId, Long cursor, Long cursorLike,
                                                Integer size, String filter, String sort) {
        FeedSort feedSort = FeedSort.from(sort);
        validatePopularCursor(feedSort, cursor, cursorLike);

        FeedQuery q = new FeedQuery();
        q.setViewerId(viewerId);
        q.setFilter(FeedFilter.from(filter));
        q.setSort(feedSort);
        q.setPublicOnly(true);
        q.setCursor(cursor);
        q.setCursorLike(cursorLike);
        q.setSize(clampSize(size));
        return certificationMapper.findFeed(q);
    }

    // 내 피드 — 내 글 전부, 필터 없음
    public List<FeedItemResponse> getMyFeed(Long viewerId, Long cursor, Integer size) {
        FeedQuery q = new FeedQuery();
        q.setTargetUserId(viewerId);   // 내 글
        q.setViewerId(viewerId);
        q.setIncludeAllStatus(true);   // 대기, 반려 다 포함
        q.setCursor(cursor);
        q.setSize(clampSize(size));
        return certificationMapper.findFeed(q);
    }

    // 타인 피드 — 유저의 승인글, 비공개 챌린지 글은 뷰어가 참가자일 때만
    // TODO: 미존재 유저면 빈 목록 주는 걸로 했는데 인호님께 확인해야할듯?
    public List<FeedItemResponse> getUserFeed(Long targetUserId, Long viewerId, Long cursor, Integer size) {
        FeedQuery q = new FeedQuery();
        q.setTargetUserId(targetUserId);
        q.setViewerId(viewerId);
        q.setApplyViewerVisibility(true);
        q.setCursor(cursor);
        q.setSize(clampSize(size));
        return certificationMapper.findFeed(q);
    }

    // [챌린지 도메인 연동 지점] 참가자별 APPROVED 인증 일수 제공.
    // 성공/실패 판정(성공률 비교)은 챌린지 도메인 관할 — 여기서는 일수만 센다.
    // 요청한 participantId 전부를 순서대로 응답하고, 인증이 없으면 successCount=0.
    public List<ParticipantSuccessCount> getSuccessCounts(List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return List.of();   // 빈 IN () 절은 SQL 오류라 쿼리 전에 차단
        }
        Map<Long, Integer> counted = certificationMapper.countApprovedDaysByParticipantIds(participantIds).stream()
                .collect(Collectors.toMap(ParticipantSuccessCount::getParticipantId,
                                          ParticipantSuccessCount::getSuccessCount));
        List<ParticipantSuccessCount> result = new ArrayList<>(participantIds.size());
        for (Long participantId : participantIds) {
            ParticipantSuccessCount item = new ParticipantSuccessCount();
            item.setParticipantId(participantId);
            item.setSuccessCount(counted.getOrDefault(participantId, 0));   // 집계에 없으면 0
            result.add(item);
        }
        return result;
    }
}
