package com.deundeun.challenge.service;

import com.deundeun.challenge.domain.CertFrequency;
import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.dto.request.ChallengeCreateRequest;
import com.deundeun.challenge.dto.request.ChallengeSearchCondition;
import com.deundeun.challenge.dto.request.ChallengeUpdateRequest;
import com.deundeun.challenge.dto.response.ChallengeCreateResponse;
import com.deundeun.challenge.dto.response.ChallengeDetailResponse;
import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;
import com.deundeun.challenge.dto.response.PageResponse;
import com.deundeun.challenge.mapper.CategoryMapper;
import com.deundeun.challenge.mapper.ChallengeMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeMapper  challengeMapper;
    private final CategoryMapper  categoryMapper;

    @Transactional
    public ChallengeCreateResponse create(Long hostId, ChallengeCreateRequest request) {
        // 규칙검증
        if (!request.endDate().isAfter(request.startDate())) {
            throw new ApiException(ErrorCode.INVALID_CHALLENGE_PERIOD);
        }
        if (!request.certEndTime().isAfter(request.certStartTime())) {
            throw new ApiException(ErrorCode.INVALID_CERT_TIME_RANGE);
        }
        if (!categoryMapper.existsById(request.categoryId())) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Challenge challenge = Challenge.builder()
                .hostId(hostId)                                    // 개설자 = 방장
                .title(request.title())
                .description(request.description())
                .categoryId(request.categoryId())
                .entryFee(request.entryFee())
                .verificationMethod(request.verificationMethod())
                .certFrequency(CertFrequency.DAILY)                // MVP 고정
                .dailyCertLimit(request.dailyCertLimit())
                .successCriteriaRate(80)                           // 80% 고정
                .aiReviewEnabled(request.aiReviewEnabled())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .maxParticipants(request.maxParticipants())
                .status(ChallengeStatus.RECRUITING)                // 생성 시 무조건 모집중
                .certStartTime(request.certStartTime())
                .certEndTime(request.certEndTime())
                .feedVisibility(request.feedVisibility())
                .build();

        challengeMapper.insert(challenge);

        return ChallengeCreateResponse.from(challenge);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChallengeSummaryResponse> search(ChallengeSearchCondition condition) {
        List<ChallengeSummaryResponse> content = challengeMapper.search(
                condition.categoryId(),
                condition.status(),
                condition.keyword(),
                condition.offset(),
                condition.size());

        long totalElements = challengeMapper.countBySearch(
                condition.categoryId(),
                condition.status(),
                condition.keyword());

        return PageResponse.of(content, condition.page(), condition.size(), totalElements);
    }

    @Transactional
    public void update(Long challengeId, Long userId, ChallengeUpdateRequest request) {
        if (!request.hasAnyChanges()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        // 행 잠금: 참가자 수 확인과 UPDATE 사이에 참가가 끼어드는 경쟁 조건 방지
        Challenge challenge = challengeMapper.findByIdForUpdate(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        if (!challenge.getHostId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        // 참가자(방장 제외)가 있으면 제목/설명 외 핵심 조건은 수정 불가
        if (request.hasCoreChanges()
                && challengeMapper.countActiveParticipantsExceptHost(challengeId) > 0) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_EDITABLE);
        }

        // 부분 수정이므로 "요청값 + 기존값"을 병합한 최종 상태로 규칙을 검증한다
        LocalDate newStartDate = request.startDate() != null ? request.startDate() : challenge.getStartDate();
        LocalDate newEndDate = request.endDate() != null ? request.endDate() : challenge.getEndDate();
        if (!newEndDate.isAfter(newStartDate)) {
            throw new ApiException(ErrorCode.INVALID_CHALLENGE_PERIOD);
        }

        LocalTime newCertStart = request.certStartTime() != null ? request.certStartTime() : challenge.getCertStartTime();
        LocalTime newCertEnd = request.certEndTime() != null ? request.certEndTime() : challenge.getCertEndTime();
        if (!newCertEnd.isAfter(newCertStart)) {
            throw new ApiException(ErrorCode.INVALID_CERT_TIME_RANGE);
        }

        if (request.categoryId() != null && !categoryMapper.existsById(request.categoryId())) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // null 필드는 동적 SQL(<set>/<if>)에서 제외되어 기존 값이 유지된다
        Challenge updateTarget = Challenge.builder()
                .id(challengeId)
                .title(request.title())
                .description(request.description())
                .categoryId(request.categoryId())
                .entryFee(request.entryFee())
                .verificationMethod(request.verificationMethod())
                .dailyCertLimit(request.dailyCertLimit())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .maxParticipants(request.maxParticipants())
                .certStartTime(request.certStartTime())
                .certEndTime(request.certEndTime())
                .feedVisibility(request.feedVisibility())
                .build();

        challengeMapper.update(updateTarget);
    }

    @Transactional
    public void cancel(Long challengeId, Long userId) {
        // 행 잠금: 상태 확인과 CANCELLED 전이 사이의 동시 요청(참가 등) 방지
        Challenge challenge = challengeMapper.findByIdForUpdate(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        if (!challenge.getHostId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (challenge.getStatus() != ChallengeStatus.RECRUITING) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_RECRUITING);
        }
        // TODO: 참가 API 구현 후, 거부 대신 참가자 전원의 참가비 홀딩을 해제(cancel)하고 취소 허용으로 교체
        if (challengeMapper.countActiveParticipantsExceptHost(challengeId) > 0) {
            throw new ApiException(ErrorCode.CHALLENGE_HAS_PARTICIPANTS);
        }

        challengeMapper.updateStatus(challengeId, ChallengeStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public ChallengeDetailResponse getDetail(Long challengeId) {
        ChallengeDetailResponse detail = challengeMapper.findDetailById(challengeId);
        if (detail == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        return detail;
    }

    @Transactional(readOnly = true)
    public List<ChallengeSummaryResponse> getMyChallenges(Long userId) {
        return challengeMapper.findMyChallenges(userId);
    }

}
