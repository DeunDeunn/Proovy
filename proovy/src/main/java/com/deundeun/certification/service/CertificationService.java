package com.deundeun.certification.service;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.ChallengeForCertification;
import com.deundeun.certification.dto.CreateCertificationPostRequest;
import com.deundeun.certification.dto.CreateCertificationPostSqlParam;
import com.deundeun.certification.dto.ParticipantForCertification;
import com.deundeun.certification.dto.PendingCertificationResponse;
import com.deundeun.certification.dto.PostReviewContext;
import com.deundeun.certification.dto.RejectCertificationPostRequest;
import com.deundeun.certification.dto.UpdateCertificationPostRequest;
import com.deundeun.certification.enums.ApprovalType;
import com.deundeun.certification.enums.CertificationStatus;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.notification.event.VerificationApprovedEvent;
import com.deundeun.notification.event.VerificationRejectedEvent;
import com.deundeun.notification.event.VerificationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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
    /*인증글 등록
    // TODO: Challenge 테이블을 셀렉트 하여, 상태와 인증 게시 가능 시간을 가져와 확인 (셀렉트)
    //  CertificationMapper에 Challenge를 셀렉트 하는 쿼리를 작성한 뒤, 서비스단에서 호출.
    //  결과의 상태와 게시가능 시간을 확인
    //  해당 유저가 ChallengeParticipant 테이블에 존재하며, 강퇴당하거나 중도에 나가지 않았는지 체크 (셀렉트)
    //  CertificationMapper에 ChallengeParticipant를 셀렉트 하는 쿼리를 작성한 뒤, 서비스단에서 호출
    //  결과가 있으면 작성 가능. 없으면 작성할 수 없음.
    //*/
    @Transactional
    public Long createCertificationPost(Long challengeId, Long userId, CreateCertificationPostRequest request){

        // 대표 이미지 필수
        if (request.getThumbnailImage() == null || request.getThumbnailImage().isBlank()) {
            throw new ApiException(ErrorCode.THUMBNAIL_REQUIRED);
        }
        // 추가 이미지 최대 3장
        if (request.getImageList() != null && request.getImageList().size() > 3) {
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

        // TODO: 인증 시간대 검증 추가 (챌린지에 시간대 컬럼 생기면)

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

        CreateCertificationPostSqlParam param = new CreateCertificationPostSqlParam(
                null,
                userId,
                participant.getId(),
                request.getContents(),
                request.getThumbnailImage()
        );
        certificationMapper.createCertificationPost(param);

        // 추가 이미지가 있으면 저장 (없으면 건너뜀 )
        List<String> imageList = request.getImageList();
        if (imageList != null && !imageList.isEmpty()) {
            certificationMapper.insertPostImages(param.getId(), imageList);
        }

        // 방장에게 인증 게시글 검수 알림
        eventPublisher.publishEvent(new VerificationSubmittedEvent(challenge.getHostId(), param.getId()));

        return param.getId();
    }

    // 인증글 상세 조회
    public CertificationPostDetailResponse getCertificationPostDetail(Long postId) {

        // 글 1건 조회
        CertificationPostDetailResponse detail = certificationMapper.findPostDetail(postId);
        if (detail == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        // 추가 이미지
        List<String> imageUrls = certificationMapper.findPostImageUrls(postId);
        detail.setImageUrls(imageUrls);

        return detail;
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
        certificationMapper.approvePost(postId, ApprovalType.MANUAL);
        // 작성자에게 "승인됨" 알림 (실제 발송은 알림 도메인이 처리)
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
        certificationMapper.rejectPost(postId, request.getReason());
        // 작성자에게 반려됐다고 알림
        eventPublisher.publishEvent(new VerificationRejectedEvent(ctx.getAuthorId(), postId));
        // TODO: 반려글 24시간 후 자동삭제 (스케줄러 — 별도)
    }

    // 검수 대기 목록 조회 (그 챌린지 방장 또는 관리자만)
    public List<PendingCertificationResponse> getPendingCertifications(Long challengeId, Long userId) {
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
        return certificationMapper.findPendingCertifications(challengeId);
    }

    // 인증글 수정 (수정하면 PENDING 회귀)
    @Transactional
    public void updateCertificationPost(Long postId, Long userId, UpdateCertificationPostRequest request) {
        // 대표 이미지 필수 + 최대 3장
        if (request.getThumbnailImage() == null || request.getThumbnailImage().isBlank()) {
            throw new ApiException(ErrorCode.THUMBNAIL_REQUIRED);
        }
        if (request.getImageList() != null && request.getImageList().size() > 3) {
            throw new ApiException(ErrorCode.TOO_MANY_IMAGES);
        }

        Long authorId = certificationMapper.findPostAuthorId(postId);
        if (authorId == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        if (!authorId.equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);   // 작성자 본인만 수정 가능
        }

        // 본문·대표이미지 수정 + PENDING 회귀
        certificationMapper.updatePost(postId, request.getContents(), request.getThumbnailImage());

        // 추가 이미지 기존 삭제 → 받은 최종 목록 재삽입
        certificationMapper.deletePostImages(postId);
        List<String> imageList = request.getImageList();
        if (imageList != null && !imageList.isEmpty()) {
            certificationMapper.insertPostImages(postId, imageList);
        }

        // TODO: 수정도 인증 시간대 안에서만 가능하도록 검증 추가
    }
}
