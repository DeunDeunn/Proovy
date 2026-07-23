"use client";

/* eslint-disable @next/next/no-img-element -- S3 썸네일 URL은 next/image 설정 대상이 아니다. */

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";
import { useMe } from "@/features/auth/hooks";
import {
  useApproveCertificationPost,
  usePendingCertifications,
  useRejectCertificationPost,
} from "@/features/certification/hooks";
import { useActiveAiTicket } from "@/features/ai/hooks";
import { getCategoryGradient, statusBadgeMap } from "./categoryVisuals";
import { CERT_TIME_MAX, CERT_TIME_MIN } from "./certTimeRange";
import { getMinStartDate } from "./challengeDateRange";
import DateField from "./DateField";
import TimeField from "./TimeField";
import {
  useCancelChallenge,
  useCategories,
  useChallenge,
  useChallengeParticipantsManage,
  useKickParticipant,
  useUpdateChallenge,
} from "./hooks";

const TABS = [
  { key: "participants", label: "참가자 관리", enabled: true },
  { key: "certifications", label: "인증 관리", enabled: true },
  { key: "settlement", label: "정산 관리", enabled: false },
  { key: "settings", label: "방 설정", enabled: true },
];

const settingsInputClassName =
  "w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-primary disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400";
const settingsLabelClassName = "mb-1 block text-xs text-gray-500";

const RoomSettingsTab = ({ challenge, challengeId }) => {
  const router = useRouter();
  const { data: categories } = useCategories();
  const updateMutation = useUpdateChallenge(challengeId);
  const cancelMutation = useCancelChallenge(challengeId);

  const [form, setForm] = useState({
    title: challenge.title,
    description: challenge.description ?? "",
    categoryId: String(challenge.categoryId ?? ""),
    entryFee: challenge.entryFee,
    verificationMethod: challenge.verificationMethod ?? "",
    startDate: challenge.startDate,
    endDate: challenge.endDate,
    maxParticipants: challenge.maxParticipants,
    certStartTime: challenge.certStartTime?.slice(0, 5) ?? "06:00",
    certEndTime: challenge.certEndTime?.slice(0, 5) ?? "22:00",
    feedVisibility: challenge.feedVisibility,
  });

  // 모집중이고, 방장 본인 외 활동 중인 참가자가 없을 때만 전체 수정 가능
  // (참가자가 한 명이라도 생기면 이미 그 조건을 보고 참가한 것이므로 제목/설명 포함 전부 수정 불가)
  const isRecruiting = challenge.status === "RECRUITING";
  const isEditable = isRecruiting && (challenge.currentParticipants ?? 1) <= 1;

  const setField = (field) => (e) => {
    const { value } = e.target;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const isPeriodValid =
    form.startDate &&
    form.startDate >= getMinStartDate() &&
    form.endDate &&
    form.endDate > form.startDate;
  const isCertTimeValid =
    form.certEndTime > form.certStartTime &&
    form.certStartTime >= CERT_TIME_MIN &&
    form.certEndTime <= CERT_TIME_MAX;
  const isFormValid = isEditable && form.title.trim() !== "" && isPeriodValid && isCertTimeValid;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!isFormValid) return;

    updateMutation.mutate({
      title: form.title,
      description: form.description,
      categoryId: Number(form.categoryId),
      entryFee: Number(form.entryFee),
      verificationMethod: form.verificationMethod,
      startDate: form.startDate,
      endDate: form.endDate,
      maxParticipants: Number(form.maxParticipants),
      certStartTime: form.certStartTime,
      certEndTime: form.certEndTime,
      feedVisibility: form.feedVisibility,
    });
  };

  const handleCancelChallenge = () => {
    if (
      window.confirm("정말 챌린지를 닫을까요? 참가자 전원의 참가비가 환불되고 챌린지가 취소돼요.")
    ) {
      cancelMutation.mutate(challengeId, {
        onSuccess: () => router.push(`/challenges/${challengeId}`),
      });
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <Card>
        <p className="mb-4 text-sm text-gray-500">
          {!isRecruiting
            ? "모집이 끝난 챌린지는 수정할 수 없어요."
            : isEditable
              ? "아직 참가자가 없어서 모든 항목을 수정할 수 있어요."
              : "참가자가 있어 수정할 수 없어요."}
        </p>
        <div className="space-y-4">
          <div>
            <label htmlFor="settings-title" className={settingsLabelClassName}>
              챌린지 제목
            </label>
            <input
              id="settings-title"
              type="text"
              value={form.title}
              onChange={setField("title")}
              disabled={!isEditable}
              className={settingsInputClassName}
            />
          </div>

          <div>
            <label htmlFor="settings-description" className={settingsLabelClassName}>
              챌린지 설명
            </label>
            <textarea
              id="settings-description"
              rows={3}
              value={form.description}
              onChange={setField("description")}
              disabled={!isEditable}
              className={`${settingsInputClassName} resize-none`}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="settings-category" className={settingsLabelClassName}>
                카테고리
              </label>
              <select
                id="settings-category"
                value={form.categoryId}
                onChange={setField("categoryId")}
                disabled={!isEditable}
                className={settingsInputClassName}
              >
                {categories?.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="settings-entry-fee" className={settingsLabelClassName}>
                참가비
              </label>
              <div className="relative">
                <input
                  id="settings-entry-fee"
                  type="number"
                  min={1000}
                  step={1000}
                  value={form.entryFee}
                  onChange={setField("entryFee")}
                  disabled={!isEditable}
                  className={`${settingsInputClassName} pr-9`}
                />
                <span className="pointer-events-none absolute top-1/2 right-3.5 -translate-y-1/2 text-sm text-gray-400">
                  원
                </span>
              </div>
            </div>
          </div>

          <div>
            <label htmlFor="settings-verification-method" className={settingsLabelClassName}>
              인증 방법
            </label>
            <input
              id="settings-verification-method"
              type="text"
              value={form.verificationMethod}
              onChange={setField("verificationMethod")}
              disabled={!isEditable}
              className={settingsInputClassName}
            />
          </div>

          <div>
            <label className={settingsLabelClassName}>진행 기간</label>
            <div className="flex items-center gap-2">
              <div className="min-w-0 flex-1">
                <DateField
                  ariaLabel="시작일"
                  min={getMinStartDate()}
                  value={form.startDate}
                  onChange={(next) => setForm((prev) => ({ ...prev, startDate: next }))}
                  disabled={!isEditable}
                />
              </div>
              <span className="shrink-0 text-gray-400">~</span>
              <div className="min-w-0 flex-1">
                <DateField
                  ariaLabel="종료일"
                  min={form.startDate}
                  value={form.endDate}
                  onChange={(next) => setForm((prev) => ({ ...prev, endDate: next }))}
                  disabled={!isEditable}
                />
              </div>
            </div>
            {isEditable && !isPeriodValid && (
              <p className="mt-1 text-xs text-danger">종료일은 시작일보다 나중이어야 해요.</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="settings-max-participants" className={settingsLabelClassName}>
                모집 정원
              </label>
              <div className="relative">
                <input
                  id="settings-max-participants"
                  type="number"
                  min={1}
                  value={form.maxParticipants}
                  onChange={setField("maxParticipants")}
                  disabled={!isEditable}
                  className={`${settingsInputClassName} pr-9`}
                />
                <span className="pointer-events-none absolute top-1/2 right-3.5 -translate-y-1/2 text-sm text-gray-400">
                  명
                </span>
              </div>
            </div>
            <div>
              <label htmlFor="settings-feed-visibility" className={settingsLabelClassName}>
                피드 공개 범위
              </label>
              <select
                id="settings-feed-visibility"
                value={form.feedVisibility}
                onChange={setField("feedVisibility")}
                disabled={!isEditable}
                className={settingsInputClassName}
              >
                <option value="PUBLIC">전체 공개</option>
                <option value="PARTICIPANTS_ONLY">참가자만 공개</option>
              </select>
            </div>
          </div>

          <div>
            <label className={settingsLabelClassName}>인증 가능 시간</label>
            <div className="flex items-center gap-2">
              <div className="min-w-0 flex-1">
                <TimeField
                  ariaLabel="인증 시작 시간"
                  min={CERT_TIME_MIN}
                  max={CERT_TIME_MAX}
                  value={form.certStartTime}
                  onChange={(next) => setForm((prev) => ({ ...prev, certStartTime: next }))}
                  disabled={!isEditable}
                />
              </div>
              <span className="shrink-0 text-gray-400">~</span>
              <div className="min-w-0 flex-1">
                <TimeField
                  ariaLabel="인증 종료 시간"
                  min={CERT_TIME_MIN}
                  max={CERT_TIME_MAX}
                  value={form.certEndTime}
                  onChange={(next) => setForm((prev) => ({ ...prev, certEndTime: next }))}
                  disabled={!isEditable}
                />
              </div>
            </div>
            {isEditable && !isCertTimeValid && (
              <p className="mt-1 text-xs text-danger">
                인증 종료 시간은 시작 시간보다 나중이어야 하고, 오전 2시 ~ 오후 11시 사이여야 해요.
              </p>
            )}
          </div>
        </div>

        {updateMutation.isError && (
          <div className="mt-4">
            <ErrorMessage error={updateMutation.error} />
          </div>
        )}
        {updateMutation.isSuccess && (
          <p className="mt-4 text-sm font-medium text-success">저장됐어요.</p>
        )}

        <div className="mt-6 flex justify-end">
          <Button type="submit" disabled={!isFormValid || updateMutation.isPending}>
            {updateMutation.isPending ? "저장하는 중..." : "저장하기"}
          </Button>
        </div>
      </Card>

      {isRecruiting && (
        <Card className="border-red-200">
          <h3 className="text-sm font-bold text-danger">위험 구역</h3>
          <p className="mt-1 text-xs text-gray-500">
            챌린지를 닫으면 참가자 전원의 참가비가 환불되고 되돌릴 수 없어요. 모집중일 때만
            가능해요.
          </p>
          {cancelMutation.isError && (
            <div className="mt-3">
              <ErrorMessage error={cancelMutation.error} />
            </div>
          )}
          <div className="mt-3">
            <Button
              type="button"
              variant="danger"
              onClick={handleCancelChallenge}
              disabled={cancelMutation.isPending}
            >
              {cancelMutation.isPending ? "닫는 중..." : "챌린지 닫기"}
            </Button>
          </div>
        </Card>
      )}
    </form>
  );
};

const PendingCertificationCard = ({
  post,
  onApprove,
  onReject,
  isApproving,
  isRejecting,
  canManualReview,
}) => (
  <div className="flex gap-4 rounded-xl border border-gray-200 p-4">
    <Link href={`/certification-posts/${post.postId}`} className="flex min-w-0 flex-1 gap-4">
      <img
        src={post.thumbnailUrl}
        alt=""
        className="h-24 w-24 shrink-0 rounded-lg bg-gray-100 object-cover"
      />
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-2">
          <p className="font-medium text-gray-800 hover:underline">{post.authorNickname}</p>
          <span className="shrink-0 text-xs text-gray-400">
            {post.createdAt?.slice(0, 16).replace("T", " ")}
          </span>
        </div>
        <p className="mt-1 line-clamp-2 text-sm text-gray-600">{post.contents}</p>
      </div>
    </Link>

    <div className="flex shrink-0 flex-col justify-end">
      <div className="flex gap-2">
        {canManualReview ? (
          <>
            <button
              type="button"
              onClick={onApprove}
              disabled={isApproving}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              {isApproving ? "승인 중..." : "승인"}
            </button>
            <button
              type="button"
              onClick={onReject}
              disabled={isRejecting}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-danger hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              {isRejecting ? "반려 중..." : "반려"}
            </button>
          </>
        ) : (
          <span className="rounded-lg bg-primary-light px-3 py-1.5 text-xs font-semibold text-primary">
            AI 자동 검수 중
          </span>
        )}
      </div>
    </div>
  </div>
);

const CertificationReviewTab = ({ challengeId, hostId, aiReviewEnabled }) => {
  const { data, isLoading, isError, error, fetchNextPage, hasNextPage, isFetchingNextPage } =
    usePendingCertifications(challengeId);
  const {
    data: activeTicket,
    isLoading: isTicketLoading,
    error: ticketError,
  } = useActiveAiTicket();
  const approveMutation = useApproveCertificationPost(challengeId);
  const rejectMutation = useRejectCertificationPost(challengeId);

  const posts = data?.pages.flat() ?? [];
  const hasActiveTicket = activeTicket?.hasActiveTicket === true;
  const usesAutomaticReview = aiReviewEnabled === true && hasActiveTicket;

  const handleApprove = (postId) => {
    approveMutation.mutate(postId);
  };

  const handleReject = (postId) => {
    const reason = window.prompt("반려 사유를 입력해주세요.");
    if (!reason) return;
    rejectMutation.mutate({ postId, reason });
  };

  if (isError) {
    return (
      <div className="rounded-xl border border-gray-200 p-5">
        <ErrorMessage error={error} />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="rounded-xl border border-gray-200">
        <Loading label="검수 대기 목록 불러오는 중..." />
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-4 rounded-xl border border-gray-200 bg-white p-4">
        <div>
          <p className="text-sm font-semibold text-gray-900">
            {usesAutomaticReview
              ? "AI 자동 검수 활성화"
              : aiReviewEnabled
                ? "AI 티켓 비활성화"
                : "방장 수동 검수"}
          </p>
          <p className="mt-1 text-xs text-gray-500">
            {usesAutomaticReview
              ? "참가자의 인증 게시물을 AI가 자동으로 검수합니다."
              : "참가자의 인증 게시물을 방장이 직접 검수합니다."}
          </p>
        </div>
        {isTicketLoading ? (
          <span className="text-xs text-gray-400">티켓 확인 중...</span>
        ) : usesAutomaticReview ? (
          <span className="shrink-0 rounded-full bg-primary-light px-3 py-1 text-xs font-semibold text-primary">
            자동 검수 사용 중
          </span>
        ) : !hasActiveTicket ? (
          <Link
            href="/mypage/tickets/store"
            className="shrink-0 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-hover"
          >
            티켓 활성화
          </Link>
        ) : (
          <span className="shrink-0 rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-600">
            수동 검수 사용 중
          </span>
        )}
      </div>

      {ticketError && <ErrorMessage error={ticketError} />}

      {posts.length === 0 ? (
        <div className="rounded-xl border border-gray-200 p-8 text-center text-sm text-gray-400">
          검수 대기 중인 인증글이 없어요.
        </div>
      ) : (
        posts.map((post) => (
          <PendingCertificationCard
            key={post.postId}
            post={post}
            onApprove={() => handleApprove(post.postId)}
            onReject={() => handleReject(post.postId)}
            isApproving={approveMutation.isPending && approveMutation.variables === post.postId}
            isRejecting={
              rejectMutation.isPending && rejectMutation.variables?.postId === post.postId
            }
            canManualReview={!usesAutomaticReview && post.authorId !== hostId}
          />
        ))
      )}
      {hasNextPage && (
        <button
          type="button"
          onClick={() => fetchNextPage()}
          disabled={isFetchingNextPage}
          className="w-full rounded-lg border border-gray-200 py-2 text-sm text-gray-500 hover:bg-gray-50"
        >
          {isFetchingNextPage ? "불러오는 중..." : "더 보기"}
        </button>
      )}
      {(approveMutation.isError || rejectMutation.isError) && (
        <ErrorMessage error={approveMutation.error ?? rejectMutation.error} />
      )}
    </div>
  );
};

const StatTile = ({ label, value }) => (
  <div className="rounded-xl border border-gray-200 p-4">
    <p className="text-xs text-gray-400">{label}</p>
    <p className="mt-1 text-lg font-bold text-gray-900">{value}</p>
  </div>
);

const getTotalDays = (startDate, endDate) =>
  Math.round((new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24)) + 1;

const ChallengeManagePage = ({ challengeId, initialTab }) => {
  const isSelectableTab = TABS.some((t) => t.key === initialTab && t.enabled);
  const [tab, setTab] = useState(isSelectableTab ? initialTab : "participants");
  const { data: me, isLoading: isMeLoading } = useMe();
  const {
    data: challenge,
    isLoading: isChallengeLoading,
    isError,
    error,
  } = useChallenge(challengeId);

  const isHost = !!me?.id && !!challenge?.hostId && me.id === challenge.hostId;

  const {
    data: participants,
    isLoading: isParticipantsLoading,
    isError: isParticipantsError,
    error: participantsError,
  } = useChallengeParticipantsManage(challengeId, { enabled: isHost });
  const kickMutation = useKickParticipant(challengeId);

  if (isMeLoading || isChallengeLoading) return <Loading label="불러오는 중..." />;
  if (isError) return <ErrorMessage error={error} />;
  if (!challenge) return null;
  if (!isHost) {
    return <ErrorMessage error={{ message: "방장만 접근할 수 있어요." }} />;
  }

  const statusBadge = statusBadgeMap[challenge.status] ?? statusBadgeMap.RECRUITING;
  const gradient = getCategoryGradient(challenge.categoryName);
  const totalDays = getTotalDays(challenge.startDate, challenge.endDate);

  const totalPending = participants?.reduce((sum, p) => sum + (p.pendingCount ?? 0), 0) ?? 0;
  const averageSuccessRate = participants?.length
    ? Math.round(
        (participants.reduce((sum, p) => sum + (p.approvedDays ?? 0) / totalDays, 0) /
          participants.length) *
          100
      )
    : 0;

  const handleKick = (userId, nickname) => {
    if (window.confirm(`${nickname}님을 강퇴할까요? 시작 전이 아니면 참가비는 환불되지 않아요.`)) {
      kickMutation.mutate(userId);
    }
  };

  return (
    <div className="mx-auto max-w-[1440px] space-y-6 pb-2">
      <Link
        href={`/challenges/${challengeId}`}
        className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        챌린지로 돌아가기
      </Link>

      <div className="flex flex-wrap items-center gap-4">
        <div
          className={`h-16 w-16 shrink-0 overflow-hidden rounded-xl ${challenge.thumbnailUrl ? "" : `bg-gradient-to-br ${gradient}`}`}
        >
          {challenge.thumbnailUrl && (
            <img src={challenge.thumbnailUrl} alt="" className="h-full w-full object-cover" />
          )}
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-primary-light px-2.5 py-1 text-xs font-semibold text-primary">
              {challenge.categoryName}
            </span>
            <span
              className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusBadge.className}`}
            >
              {statusBadge.label}
            </span>
          </div>
          <h1 className="mt-1 text-xl font-bold text-gray-900">{challenge.title}</h1>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <StatTile
          label="참가자"
          value={`${challenge.currentParticipants} / ${challenge.maxParticipants}명`}
        />
        <StatTile label="평균 성공률" value={`${averageSuccessRate}%`} />
        <StatTile label="검수 대기" value={`${totalPending}건`} />
      </div>

      <div className="flex gap-2 border-b border-gray-200">
        {TABS.map(({ key, label, enabled }) => (
          <button
            key={key}
            type="button"
            disabled={!enabled}
            onClick={() => enabled && setTab(key)}
            className={`px-3 py-2.5 text-sm font-medium ${
              !enabled
                ? "cursor-not-allowed text-gray-300"
                : tab === key
                  ? "border-b-2 border-primary font-semibold text-primary"
                  : "text-gray-500 hover:text-gray-700"
            }`}
          >
            {label}
            {!enabled && <span className="ml-1 text-[10px] text-gray-300">(준비 중)</span>}
          </button>
        ))}
      </div>

      {tab === "participants" && (
        <div className="rounded-xl border border-gray-200">
          {isParticipantsError ? (
            <div className="p-5">
              <ErrorMessage error={participantsError} />
            </div>
          ) : isParticipantsLoading ? (
            <Loading label="참가자 불러오는 중..." />
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-left text-xs text-gray-400">
                  <th className="p-4 font-medium">참가자</th>
                  <th className="p-4 font-medium">참가일</th>
                  <th className="p-4 font-medium">진행률</th>
                  <th className="p-4 font-medium">검수 대기</th>
                  <th className="p-4 font-medium">관리</th>
                </tr>
              </thead>
              <tbody>
                {(participants ?? []).map((p) => {
                  const progressPercent = Math.min(
                    100,
                    Math.round(((p.approvedDays ?? 0) / totalDays) * 100)
                  );
                  const isParticipantHost = p.userId === challenge.hostId;
                  return (
                    <tr key={p.userId} className="border-b border-gray-100 last:border-0">
                      <td className="p-4 font-medium text-gray-800">
                        {p.nickname}
                        {isParticipantHost && (
                          <span className="ml-1 text-xs text-gray-400">(방장)</span>
                        )}
                      </td>
                      <td className="p-4 text-gray-500">{p.joinedAt?.slice(0, 10)}</td>
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <div className="h-1.5 w-24 rounded-full bg-gray-100">
                            <div
                              className="h-1.5 rounded-full bg-primary"
                              style={{ width: `${progressPercent}%` }}
                            />
                          </div>
                          <span className="text-xs text-gray-500">
                            {p.approvedDays ?? 0}/{totalDays}
                          </span>
                        </div>
                      </td>
                      <td className="p-4">
                        {p.pendingCount > 0 ? (
                          <span className="font-semibold text-orange-500">{p.pendingCount}건</span>
                        ) : (
                          <span className="text-gray-400">0건</span>
                        )}
                      </td>
                      <td className="p-4">
                        <button
                          type="button"
                          disabled={isParticipantHost || kickMutation.isPending}
                          onClick={() => handleKick(p.userId, p.nickname)}
                          className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          {kickMutation.isPending && kickMutation.variables === p.userId
                            ? "강퇴 중..."
                            : "강퇴"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
                {(participants ?? []).length === 0 && (
                  <tr>
                    <td colSpan={5} className="p-8 text-center text-gray-400">
                      아직 참가자가 없어요.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
          {kickMutation.isError && (
            <div className="p-5 pt-0">
              <ErrorMessage error={kickMutation.error} />
            </div>
          )}
        </div>
      )}

      {tab === "certifications" && (
        <CertificationReviewTab
          challengeId={challengeId}
          hostId={challenge.hostId}
          aiReviewEnabled={challenge.aiReviewEnabled}
        />
      )}

      {tab === "settings" && <RoomSettingsTab challenge={challenge} challengeId={challengeId} />}
    </div>
  );
};

export default ChallengeManagePage;
