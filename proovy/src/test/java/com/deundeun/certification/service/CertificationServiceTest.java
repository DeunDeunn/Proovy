package com.deundeun.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.deundeun.certification.dto.CertificationPostDetailResponse;
import com.deundeun.certification.dto.CertificationStreakResponse;
import com.deundeun.certification.dto.ChallengeForCertification;
import com.deundeun.certification.dto.CreateCertificationPostRequest;
import com.deundeun.certification.dto.CreateCertificationPostSqlParam;
import com.deundeun.certification.dto.FeedQuery;
import com.deundeun.certification.dto.LikeToggleResponse;
import com.deundeun.certification.dto.ParticipantForCertification;
import com.deundeun.certification.dto.ParticipantSuccessCount;
import com.deundeun.certification.dto.PostReviewContext;
import com.deundeun.certification.dto.RejectCertificationPostRequest;
import com.deundeun.certification.dto.UpdateCertificationPostRequest;
import com.deundeun.certification.enums.ApprovalType;
import com.deundeun.certification.enums.CertificationStatus;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.file.TransactionalFileUploader;
import com.deundeun.notification.event.VerificationApprovedEvent;
import com.deundeun.notification.event.VerificationRejectedEvent;
import com.deundeun.notification.event.VerificationSubmittedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("CertificationService")
class CertificationServiceTest {

    @Mock
    private CertificationMapper certificationMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private TransactionalFileUploader transactionalFileUploader;

    @InjectMocks
    private CertificationService certificationService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Long POST_ID = 1L;
    private static final Long CHALLENGE_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Long HOST_ID = 200L;

    private MultipartFile file() {
        return new MockMultipartFile("f", "n.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private MultipartFile emptyFile() {
        return new MockMultipartFile("f", "", "image/jpeg", new byte[0]);
    }

    // 현재 시각(KST)이 항상 포함되는 인증 시간대
    private ChallengeForCertification challengeInRange(String status) {
        ChallengeForCertification c = new ChallengeForCertification();
        c.setId(CHALLENGE_ID);
        c.setStatus(status);
        c.setHostId(HOST_ID);
        c.setCertStartTime(LocalTime.MIN);
        c.setCertEndTime(LocalTime.MAX);
        return c;
    }

    // 현재 시각(KST)을 확실히 벗어나는 인증 시간대 (자정 경계 무관하게 결정적)
    private ChallengeForCertification challengeOutOfRange() {
        ChallengeForCertification c = challengeInRange("IN_PROGRESS");
        LocalTime now = LocalTime.now(KST);
        if (now.isAfter(LocalTime.NOON)) {
            c.setCertStartTime(LocalTime.of(0, 0));
            c.setCertEndTime(LocalTime.of(0, 1));   // now > end
        } else {
            c.setCertStartTime(LocalTime.of(23, 58));
            c.setCertEndTime(LocalTime.of(23, 59));  // now < start
        }
        return c;
    }

    private ParticipantForCertification participant(long id, String status) {
        ParticipantForCertification p = new ParticipantForCertification();
        p.setId(id);
        p.setStatus(status);
        return p;
    }

    private CertificationPostDetailResponse detail(long authorId, CertificationStatus status) {
        CertificationPostDetailResponse d = new CertificationPostDetailResponse();
        d.setPostId(POST_ID);
        d.setAuthorId(authorId);
        d.setStatus(status);
        d.setThumbnailUrl("old.jpg");
        return d;
    }

    private CreateCertificationPostRequest createReq() {
        CreateCertificationPostRequest r = new CreateCertificationPostRequest();
        r.setContents("본문");
        return r;
    }

    private PostReviewContext reviewCtx(CertificationStatus status) {
        PostReviewContext ctx = new PostReviewContext();
        ctx.setPostId(POST_ID);
        ctx.setStatus(status);
        ctx.setHostId(HOST_ID);
        ctx.setAuthorId(USER_ID);
        return ctx;
    }

    @Nested
    @DisplayName("createCertificationPost")
    class Create {
        @Test
        @DisplayName("[C-01] 대표이미지가 없으면 THUMBNAIL_REQUIRED")
        void thumbnailRequired() {
            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), emptyFile(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.THUMBNAIL_REQUIRED);
        }

        @Test
        @DisplayName("[C-02] 추가이미지가 4장 이상이면 TOO_MANY_IMAGES")
        void tooManyImages() {
            List<MultipartFile> four = List.of(file(), file(), file(), file());
            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), four))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.TOO_MANY_IMAGES);
        }

        @Test
        @DisplayName("[C-03] 챌린지가 없으면 CHALLENGE_NOT_FOUND")
        void challengeNotFound() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-04] 진행중이 아니면 CHALLENGE_NOT_IN_PROGRESS")
        void notInProgress() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("RECRUITING"));

            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CHALLENGE_NOT_IN_PROGRESS);
        }

        @Test
        @DisplayName("[C-05] 인증 시간대 밖이면 NOT_IN_CERT_TIME_RANGE")
        void notInTimeRange() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeOutOfRange());

            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_IN_CERT_TIME_RANGE);
        }

        @Test
        @DisplayName("[C-06] 참가자가 아니면 NOT_CHALLENGE_PARTICIPANT")
        void notParticipant() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_CHALLENGE_PARTICIPANT);
        }

        @Test
        @DisplayName("[C-07] 참가자가 ACTIVE가 아니면 PARTICIPANT_NOT_ACTIVE")
        void participantNotActive() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(participant(5, "WITHDRAWN"));

            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PARTICIPANT_NOT_ACTIVE);
        }

        @Test
        @DisplayName("[C-08] 오늘 이미 등록했으면 ALREADY_CERTIFIED_TODAY")
        void alreadyToday() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(participant(5, "ACTIVE"));
            when(certificationMapper.countTodayCertification(5L)).thenReturn(1);

            assertThatThrownBy(() -> certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_CERTIFIED_TODAY);
        }

        @Test
        @DisplayName("[C-09] 추가이미지 포함 정상 등록 → 글+이미지 저장, Submitted 이벤트, id 반환")
        void successWithImages() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(participant(5, "ACTIVE"));
            when(certificationMapper.countTodayCertification(5L)).thenReturn(0);
            when(transactionalFileUploader.upload(any(), any())).thenReturn("thumb.jpg");
            when(transactionalFileUploader.uploadAll(any(), any())).thenReturn(List.of("img1.jpg"));
            doAnswer(inv -> {
                CreateCertificationPostSqlParam p = inv.getArgument(0);
                p.setId(555L);
                return null;
            }).when(certificationMapper).createCertificationPost(any());

            Long id = certificationService.createCertificationPost(
                    CHALLENGE_ID, USER_ID, createReq(), file(), List.of(file()));

            assertThat(id).isEqualTo(555L);
            verify(certificationMapper).insertPostImages(555L, List.of("img1.jpg"));
            verify(eventPublisher).publishEvent(any(VerificationSubmittedEvent.class));
        }

        @Test
        @DisplayName("[C-10] 추가이미지가 없으면 insertPostImages를 호출하지 않는다")
        void successNoImages() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(participant(5, "ACTIVE"));
            when(certificationMapper.countTodayCertification(5L)).thenReturn(0);
            when(transactionalFileUploader.upload(any(), any())).thenReturn("thumb.jpg");
            doAnswer(inv -> {
                CreateCertificationPostSqlParam p = inv.getArgument(0);
                p.setId(555L);
                return null;
            }).when(certificationMapper).createCertificationPost(any());

            certificationService.createCertificationPost(CHALLENGE_ID, USER_ID, createReq(), file(), null);

            verify(certificationMapper, never()).insertPostImages(anyLong(), any());
            verify(transactionalFileUploader, never()).uploadAll(any(), any());
        }
    }

    @Nested
    @DisplayName("getCertificationPostDetail / assertPostReadable (읽기 게이트)")
    class ReadGate {
        @Test
        @DisplayName("[C-11] 글이 없으면 POST_NOT_FOUND")
        void notFound() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.getCertificationPostDetail(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-12] 작성자 본인은 미승인 글도 조회할 수 있다")
        void authorReadsPending() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(USER_ID, CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);
            when(certificationMapper.findPostImageUrls(POST_ID)).thenReturn(List.of("a.jpg"));
            when(certificationMapper.existsLike(POST_ID, USER_ID)).thenReturn(true);

            CertificationPostDetailResponse res = certificationService.getCertificationPostDetail(POST_ID, USER_ID);

            assertThat(res.getImageUrls()).containsExactly("a.jpg");
            assertThat(res.isLiked()).isTrue();
        }

        @Test
        @DisplayName("[C-13] 제3자는 미승인 글을 볼 수 없다(POST_NOT_FOUND로 은폐)")
        void strangerCannotReadPending() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(999L, CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);
            ChallengeForCertification challenge = challengeInRange("IN_PROGRESS"); // hostId=HOST_ID
            when(certificationMapper.findChallengeByPostId(POST_ID)).thenReturn(challenge);

            assertThatThrownBy(() -> certificationService.getCertificationPostDetail(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-14] 관리자는 상태 무관하게 통과한다")
        void adminReads() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(999L, CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(1);

            certificationService.assertPostReadable(POST_ID, USER_ID);  // 예외 없이 통과
        }

        @Test
        @DisplayName("[C-15] 그 챌린지 방장은 상태 무관하게 통과한다")
        void hostReads() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(999L, CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(HOST_ID)).thenReturn(0);
            when(certificationMapper.findChallengeByPostId(POST_ID)).thenReturn(challengeInRange("IN_PROGRESS"));

            certificationService.assertPostReadable(POST_ID, HOST_ID);  // 방장 → 통과
        }

        @Test
        @DisplayName("[C-16] 참가자공개 챌린지에서 비참가자는 볼 수 없다")
        void participantsOnlyBlocksStranger() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(999L, CertificationStatus.APPROVED));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);
            ChallengeForCertification challenge = challengeInRange("IN_PROGRESS");
            challenge.setFeedVisibility("PARTICIPANTS_ONLY");
            when(certificationMapper.findChallengeByPostId(POST_ID)).thenReturn(challenge);
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.assertPostReadable(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("toggleLike")
    class Like {
        @Test
        @DisplayName("[C-17] 글이 없으면 POST_NOT_FOUND")
        void notFound() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.toggleLike(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-18] 미승인 글에는 좋아요할 수 없다(CANNOT_LIKE_UNAPPROVED)")
        void unapproved() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(USER_ID, CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0); // 작성자라 게이트는 통과

            assertThatThrownBy(() -> certificationService.toggleLike(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_LIKE_UNAPPROVED);
        }

        @Test
        @DisplayName("[C-19] 좋아요 등록 → liked=true, like_count 증가")
        void likeOn() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(USER_ID, CertificationStatus.APPROVED));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);
            when(certificationMapper.deleteLike(POST_ID, USER_ID)).thenReturn(0);   // 기존 없음
            when(certificationMapper.insertLike(POST_ID, USER_ID)).thenReturn(1);
            when(certificationMapper.findLikeCount(POST_ID)).thenReturn(1L);

            LikeToggleResponse res = certificationService.toggleLike(POST_ID, USER_ID);

            assertThat(res.isLiked()).isTrue();
            assertThat(res.getLikeCount()).isEqualTo(1L);
            verify(certificationMapper).incrementLikeCount(POST_ID);
        }

        @Test
        @DisplayName("[C-20] 좋아요 취소 → liked=false, like_count 감소")
        void likeOff() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(USER_ID, CertificationStatus.APPROVED));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);
            when(certificationMapper.deleteLike(POST_ID, USER_ID)).thenReturn(1);   // 기존 있음 → 취소
            when(certificationMapper.findLikeCount(POST_ID)).thenReturn(0L);

            LikeToggleResponse res = certificationService.toggleLike(POST_ID, USER_ID);

            assertThat(res.isLiked()).isFalse();
            verify(certificationMapper).decrementLikeCount(POST_ID);
            verify(certificationMapper, never()).insertLike(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("deleteCertificationPost")
    class Delete {
        @Test
        @DisplayName("[C-21] 글이 없으면 POST_NOT_FOUND")
        void notFound() {
            when(certificationMapper.findPostAuthorId(POST_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.deleteCertificationPost(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-22] 작성자·관리자가 아니면 FORBIDDEN")
        void forbidden() {
            when(certificationMapper.findPostAuthorId(POST_ID)).thenReturn(999L);
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> certificationService.deleteCertificationPost(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[C-23] 작성자면 soft delete")
        void success() {
            when(certificationMapper.findPostAuthorId(POST_ID)).thenReturn(USER_ID);
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            certificationService.deleteCertificationPost(POST_ID, USER_ID);

            verify(certificationMapper).softDeletePost(POST_ID);
        }
    }

    @Nested
    @DisplayName("approveCertificationPost")
    class Approve {
        @Test
        @DisplayName("[C-24] 글이 없으면 POST_NOT_FOUND")
        void notFound() {
            when(certificationMapper.findPostReviewContext(POST_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.approveCertificationPost(POST_ID, HOST_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-25] 방장·관리자가 아니면 FORBIDDEN")
        void forbidden() {
            when(certificationMapper.findPostReviewContext(POST_ID)).thenReturn(reviewCtx(CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> certificationService.approveCertificationPost(POST_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[C-26] PENDING이 아니면 NOT_PENDING_POST")
        void notPending() {
            when(certificationMapper.findPostReviewContext(POST_ID)).thenReturn(reviewCtx(CertificationStatus.APPROVED));
            when(certificationMapper.isAdmin(HOST_ID)).thenReturn(0);

            assertThatThrownBy(() -> certificationService.approveCertificationPost(POST_ID, HOST_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_PENDING_POST);
        }

        @Test
        @DisplayName("[C-27] 방장이 승인하면 approvePost(MANUAL) + Approved 이벤트")
        void success() {
            when(certificationMapper.findPostReviewContext(POST_ID)).thenReturn(reviewCtx(CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(HOST_ID)).thenReturn(0);
            when(certificationMapper.approvePost(POST_ID, ApprovalType.MANUAL)).thenReturn(1);

            certificationService.approveCertificationPost(POST_ID, HOST_ID);

            verify(certificationMapper).approvePost(POST_ID, ApprovalType.MANUAL);
            verify(eventPublisher).publishEvent(any(VerificationApprovedEvent.class));
        }

        @Test
        @DisplayName("[C-28] 동시성으로 0행이면 NOT_PENDING_POST, 이벤트 미발행")
        void raceCondition() {
            when(certificationMapper.findPostReviewContext(POST_ID)).thenReturn(reviewCtx(CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(HOST_ID)).thenReturn(0);
            when(certificationMapper.approvePost(POST_ID, ApprovalType.MANUAL)).thenReturn(0);

            assertThatThrownBy(() -> certificationService.approveCertificationPost(POST_ID, HOST_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_PENDING_POST);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("rejectCertificationPost")
    class Reject {
        private RejectCertificationPostRequest reason(String r) {
            RejectCertificationPostRequest req = new RejectCertificationPostRequest();
            req.setReason(r);
            return req;
        }

        @Test
        @DisplayName("[C-29] 사유가 비면 REJECTION_REASON_REQUIRED")
        void reasonRequired() {
            assertThatThrownBy(() -> certificationService.rejectCertificationPost(POST_ID, HOST_ID, reason("  ")))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REJECTION_REASON_REQUIRED);
        }

        @Test
        @DisplayName("[C-30] 방장·관리자가 아니면 FORBIDDEN")
        void forbidden() {
            when(certificationMapper.findPostReviewContext(POST_ID)).thenReturn(reviewCtx(CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> certificationService.rejectCertificationPost(POST_ID, USER_ID, reason("사유")))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[C-31] 방장이 반려하면 rejectPost + Rejected 이벤트")
        void success() {
            when(certificationMapper.findPostReviewContext(POST_ID)).thenReturn(reviewCtx(CertificationStatus.PENDING));
            when(certificationMapper.isAdmin(HOST_ID)).thenReturn(0);
            when(certificationMapper.rejectPost(POST_ID, "사유")).thenReturn(1);

            certificationService.rejectCertificationPost(POST_ID, HOST_ID, reason("사유"));

            verify(certificationMapper).rejectPost(POST_ID, "사유");
            verify(eventPublisher).publishEvent(any(VerificationRejectedEvent.class));
        }
    }

    @Nested
    @DisplayName("getPendingCertifications")
    class Pending {
        @Test
        @DisplayName("[C-32] 챌린지가 없으면 CHALLENGE_NOT_FOUND")
        void challengeNotFound() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.getPendingCertifications(CHALLENGE_ID, HOST_ID, null, 20))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-33] 방장·관리자가 아니면 FORBIDDEN")
        void forbidden() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> certificationService.getPendingCertifications(CHALLENGE_ID, USER_ID, null, 20))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[C-34] 방장이면 목록을 반환한다(size 클램프 적용)")
        void success() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findPendingCertifications(eq(CHALLENGE_ID), any(), eq(50)))
                    .thenReturn(List.of());

            certificationService.getPendingCertifications(CHALLENGE_ID, HOST_ID, null, 999); // 999 → 50 클램프

            verify(certificationMapper).findPendingCertifications(CHALLENGE_ID, null, 50);
        }
    }

    @Nested
    @DisplayName("updateCertificationPost")
    class Update {
        private UpdateCertificationPostRequest req() {
            UpdateCertificationPostRequest r = new UpdateCertificationPostRequest();
            r.setContents("수정본");
            return r;
        }

        @Test
        @DisplayName("[C-35] 대표이미지가 없으면 THUMBNAIL_REQUIRED")
        void thumbnailRequired() {
            assertThatThrownBy(() -> certificationService.updateCertificationPost(
                    POST_ID, USER_ID, req(), emptyFile(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.THUMBNAIL_REQUIRED);
        }

        @Test
        @DisplayName("[C-36] 글이 없으면 POST_NOT_FOUND")
        void notFound() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.updateCertificationPost(
                    POST_ID, USER_ID, req(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("[C-37] 작성자가 아니면 FORBIDDEN")
        void forbidden() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(999L, CertificationStatus.APPROVED));

            assertThatThrownBy(() -> certificationService.updateCertificationPost(
                    POST_ID, USER_ID, req(), file(), null))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[C-38] 작성자가 수정하면 updatePost + 이미지 재삽입")
        void success() {
            when(certificationMapper.findPostDetail(POST_ID)).thenReturn(detail(USER_ID, CertificationStatus.APPROVED));
            when(certificationMapper.findChallengeByPostId(POST_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(transactionalFileUploader.uploadReplacing(any(), any(), any())).thenReturn("new.jpg");
            when(transactionalFileUploader.uploadAll(any(), any())).thenReturn(List.of("i1.jpg"));

            certificationService.updateCertificationPost(POST_ID, USER_ID, req(), file(), List.of(file()));

            verify(certificationMapper).updatePost(POST_ID, "수정본", "new.jpg");
            verify(certificationMapper).deletePostImages(POST_ID);
            verify(certificationMapper).insertPostImages(POST_ID, List.of("i1.jpg"));
        }
    }

    @Nested
    @DisplayName("피드 조회")
    class Feed {
        @Test
        @DisplayName("[C-39] getChallengeFeed: 참가자가 아니면 NOT_CHALLENGE_PARTICIPANT")
        void notParticipant() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> certificationService.getChallengeFeed(
                    CHALLENGE_ID, USER_ID, null, null, 20, "all", "latest"))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_CHALLENGE_PARTICIPANT);
        }

        @Test
        @DisplayName("[C-40] getChallengeFeed: 인기순 커서 부분입력이면 INVALID_POPULAR_CURSOR")
        void invalidPopularCursor() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID)).thenReturn(participant(5, "ACTIVE"));

            // sort=popular인데 cursor만 있고 cursorLike는 없음
            assertThatThrownBy(() -> certificationService.getChallengeFeed(
                    CHALLENGE_ID, USER_ID, 99L, null, 20, "all", "popular"))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_POPULAR_CURSOR);
        }

        @Test
        @DisplayName("[C-41] getChallengeFeed: review 필터는 방장이 참가자 여부와 무관하게 오래된 순으로 조회한다")
        void reviewFeedForHost() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.findFeed(any())).thenReturn(List.of());

            certificationService.getChallengeFeed(
                    CHALLENGE_ID, HOST_ID, 10L, 999L, 20, "review", "popular");

            ArgumentCaptor<FeedQuery> captor = ArgumentCaptor.forClass(FeedQuery.class);
            verify(certificationMapper).findFeed(captor.capture());
            FeedQuery q = captor.getValue();
            assertThat(q.isReviewMode()).isTrue();
            assertThat(q.getFilter().name()).isEqualTo("REVIEW");
            assertThat(q.getSort().name()).isEqualTo("LATEST");
            assertThat(q.getCursor()).isEqualTo(10L);
            assertThat(q.getCursorLike()).isNull();
            verify(certificationMapper, never()).findParticipant(CHALLENGE_ID, HOST_ID);
        }

        @Test
        @DisplayName("[C-42] getChallengeFeed: review 필터는 방장·관리자가 아니면 FORBIDDEN")
        void reviewFeedForbiddenForRegularUser() {
            when(certificationMapper.findChallengeById(CHALLENGE_ID)).thenReturn(challengeInRange("IN_PROGRESS"));
            when(certificationMapper.isAdmin(USER_ID)).thenReturn(0);

            assertThatThrownBy(() -> certificationService.getChallengeFeed(
                    CHALLENGE_ID, USER_ID, null, null, 20, "review", "latest"))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("[C-43] getMyFeed: size가 null이면 기본 20으로 클램프되어 조회한다")
        void myFeedClampsSize() {
            when(certificationMapper.findFeed(any())).thenReturn(List.of());

            certificationService.getMyFeed(USER_ID, null, null);

            ArgumentCaptor<FeedQuery> captor = ArgumentCaptor.forClass(FeedQuery.class);
            verify(certificationMapper).findFeed(captor.capture());
            FeedQuery q = captor.getValue();
            assertThat(q.getSize()).isEqualTo(20);
            assertThat(q.isIncludeAllStatus()).isTrue();
            assertThat(q.getTargetUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("[C-42] getPublicFeed: publicOnly=true로 조회한다")
        void publicFeed() {
            when(certificationMapper.findFeed(any())).thenReturn(List.of());

            certificationService.getPublicFeed(USER_ID, null, null, 20, "all", "latest");

            ArgumentCaptor<FeedQuery> captor = ArgumentCaptor.forClass(FeedQuery.class);
            verify(certificationMapper).findFeed(captor.capture());
            assertThat(captor.getValue().isPublicOnly()).isTrue();
        }
    }

    @Nested
    @DisplayName("getSuccessCounts (챌린지 도메인 연동)")
    class SuccessCounts {
        @Test
        @DisplayName("[C-43] null/빈 입력이면 빈 리스트, mapper를 호출하지 않는다")
        void emptyInput() {
            assertThat(certificationService.getSuccessCounts(null)).isEmpty();
            assertThat(certificationService.getSuccessCounts(List.of())).isEmpty();
            verify(certificationMapper, never()).countApprovedDaysByParticipantIds(any());
        }

        @Test
        @DisplayName("[C-44] 요청 순서대로 반환하고 집계 없는 참가자는 0으로 채운다")
        void fillsZeroInOrder() {
            ParticipantSuccessCount counted = new ParticipantSuccessCount();
            counted.setParticipantId(2L);
            counted.setSuccessCount(5);
            when(certificationMapper.countApprovedDaysByParticipantIds(List.of(1L, 2L, 3L)))
                    .thenReturn(List.of(counted));   // 2번만 실적 있음

            List<ParticipantSuccessCount> result = certificationService.getSuccessCounts(List.of(1L, 2L, 3L));

            assertThat(result).extracting(ParticipantSuccessCount::getParticipantId)
                    .containsExactly(1L, 2L, 3L);   // 요청 순서 유지
            assertThat(result).extracting(ParticipantSuccessCount::getSuccessCount)
                    .containsExactly(0, 5, 0);      // 없는 참가자는 0
        }
    }

    @Nested
    @DisplayName("getMyCertificationStreak")
    class CertificationStreak {
        private static final Long PARTICIPANT_ID = 5L;

        private void activeParticipant() {
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID))
                    .thenReturn(participant(PARTICIPANT_ID, "ACTIVE"));
        }

        @Test
        @DisplayName("[C-45] 오늘부터 끊기지 않은 승인 인증일 수를 반환한다")
        void countsStreakFromToday() {
            LocalDate today = LocalDate.now(KST);
            activeParticipant();
            when(certificationMapper.findApprovedCertificationDates(PARTICIPANT_ID))
                    .thenReturn(List.of(today, today.minusDays(1), today.minusDays(2)));

            CertificationStreakResponse result =
                    certificationService.getMyCertificationStreak(CHALLENGE_ID, USER_ID);

            assertThat(result.currentStreakDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("[C-46] 오늘 미승인이어도 어제까지 이어진 연속일을 유지한다")
        void keepsYesterdayStreakWhenTodayIsNotApproved() {
            LocalDate today = LocalDate.now(KST);
            activeParticipant();
            when(certificationMapper.findApprovedCertificationDates(PARTICIPANT_ID))
                    .thenReturn(List.of(today.minusDays(1), today.minusDays(2)));

            CertificationStreakResponse result =
                    certificationService.getMyCertificationStreak(CHALLENGE_ID, USER_ID);

            assertThat(result.currentStreakDays()).isEqualTo(2);
        }

        @Test
        @DisplayName("[C-47] 어제 승인 인증이 없으면 연속일은 0이다")
        void returnsZeroWhenYesterdayIsMissing() {
            LocalDate today = LocalDate.now(KST);
            activeParticipant();
            when(certificationMapper.findApprovedCertificationDates(PARTICIPANT_ID))
                    .thenReturn(List.of(today.minusDays(2)));

            CertificationStreakResponse result =
                    certificationService.getMyCertificationStreak(CHALLENGE_ID, USER_ID);

            assertThat(result.currentStreakDays()).isZero();
        }

        @Test
        @DisplayName("[C-48] 챌린지 ACTIVE 참가자가 아니면 조회할 수 없다")
        void rejectsNonActiveParticipant() {
            when(certificationMapper.findParticipant(CHALLENGE_ID, USER_ID))
                    .thenReturn(participant(PARTICIPANT_ID, "WITHDRAWN"));

            assertThatThrownBy(() -> certificationService.getMyCertificationStreak(CHALLENGE_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PARTICIPANT_NOT_ACTIVE);
            verify(certificationMapper, never()).findApprovedCertificationDates(anyLong());
        }
    }
}
