"use client";

/* eslint-disable @next/next/no-img-element -- S3 썸네일 URL은 next/image 설정 대상이 아니다. */

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";

import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";
import { useMe } from "@/features/auth/hooks";
import { getCategoryGradient, statusBadgeMap } from "./categoryVisuals";
import { useChallenge, useChallengeParticipantsManage, useKickParticipant } from "./hooks";

const TABS = [
  { key: "participants", label: "참가자 관리", enabled: true },
  { key: "certifications", label: "인증 관리", enabled: false },
  { key: "settlement", label: "정산 관리", enabled: false },
  { key: "settings", label: "방 설정", enabled: false },
];

const StatTile = ({ label, value }) => (
  <div className="rounded-xl border border-gray-200 p-4">
    <p className="text-xs text-gray-400">{label}</p>
    <p className="mt-1 text-lg font-bold text-gray-900">{value}</p>
  </div>
);

const getTotalDays = (startDate, endDate) =>
  Math.round((new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24)) + 1;

const ChallengeManagePage = ({ challengeId }) => {
  const [tab, setTab] = useState("participants");
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
              className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${statusBadge.className}`}
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
                    Math.round((p.approvedDays / totalDays) * 100)
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
                            {p.approvedDays}/{totalDays}
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
    </div>
  );
};

export default ChallengeManagePage;
