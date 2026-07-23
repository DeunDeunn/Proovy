"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ArrowLeft,
  Award,
  Bot,
  Calendar,
  Camera,
  ChevronDown,
  ChevronUp,
  Share2,
  Target,
  Users,
  Wallet,
} from "lucide-react";

import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";
import { DEFAULT_PROFILE_IMAGE_URL } from "@/lib/constants";
import { formatChallengePeriod } from "@/lib/date";
import { useMe } from "@/features/auth/hooks";
import { useUserProfile } from "@/features/users/hooks";
import { useWallet } from "@/features/wallet/hooks";
import { statusBadgeMap } from "./categoryVisuals";
import {
  useChallenge,
  useChallengeParticipants,
  useJoinChallenge,
  useLeaveChallenge,
  useUpdateChallengeThumbnail,
} from "./hooks";

const PARTICIPANT_PREVIEW_COUNT = 5;

const InfoRow = ({ icon: Icon, label, value }) => (
  <div className="flex items-center gap-3">
    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-light text-primary">
      <Icon size={16} />
    </div>
    <div className="min-w-0">
      <p className="text-xs text-gray-400">{label}</p>
      <p className="mt-0.5 truncate text-sm font-semibold text-gray-800">{value}</p>
    </div>
  </div>
);

const ChallengeDetailPage = ({ challengeId }) => {
  const [showFullDescription, setShowFullDescription] = useState(false);
  const [isDescriptionClampable, setIsDescriptionClampable] = useState(false);
  const [showAllParticipants, setShowAllParticipants] = useState(false);
  const [linkCopied, setLinkCopied] = useState(false);
  const router = useRouter();
  const searchParams = useSearchParams();
  const descriptionRef = useRef(null);

  const { data: me } = useMe();
  const { data: challenge, isLoading, isError, error } = useChallenge(challengeId);

  // 설명이 3줄(line-clamp-3)을 넘겨서 실제로 잘렸을 때만 더보기/접기 버튼을 보여준다.
  // 창 크기 조절/기기 회전으로 줄바꿈이 달라지면 잘림 여부도 바뀌므로 리사이즈 때도 다시 잰다
  useEffect(() => {
    const recalc = () => {
      const el = descriptionRef.current;
      if (!el) return;
      setIsDescriptionClampable(el.scrollHeight > el.clientHeight + 1);
    };
    recalc();
    window.addEventListener("resize", recalc);
    return () => window.removeEventListener("resize", recalc);
  }, [challenge?.description]);
  const { data: hostProfile } = useUserProfile(challenge?.hostId);
  const { data: wallet, isLoading: isWalletLoading } = useWallet({ enabled: !!me });
  const { data: participants } = useChallengeParticipants(challengeId);

  const joinMutation = useJoinChallenge(challengeId);
  const leaveMutation = useLeaveChallenge(challengeId);
  const thumbnailMutation = useUpdateChallengeThumbnail();
  const thumbnailInputRef = useRef(null);

  if (isLoading) return <Loading label="챌린지 불러오는 중..." />;
  if (isError) return <ErrorMessage error={error} />;
  if (!challenge) return null;

  const statusBadge = statusBadgeMap[challenge.status] ?? statusBadgeMap.RECRUITING;

  const isHost = me?.id != null && me.id === challenge.hostId;
  const isActiveParticipant =
    participants?.some((p) => p.userId === me?.id && p.status === "ACTIVE") ?? false;
  const isParticipant = isActiveParticipant;
  const canUseCertification = challenge.status === "IN_PROGRESS" && isActiveParticipant;
  const isFull = challenge.currentParticipants >= challenge.maxParticipants;
  const hasEnoughCash = wallet?.availableBalance >= challenge.entryFee;
  const thumbnailUploadFailed = searchParams.get("thumbnailUpload") === "failed";

  // 진행 기간 대비 남은 일수/진행률 - 참가자에게 참가비/보유 캐시 대신 보여줄 요약 정보
  const startDate = new Date(challenge.startDate);
  const endDate = new Date(challenge.endDate);
  const totalDays = Math.round((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;
  // eslint-disable-next-line react-hooks/purity -- 남은 일수는 현재 시각 기준 표시용 값이라 렌더마다 달라져도 무방하다
  const daysLeft = Math.max(0, Math.ceil((endDate - Date.now()) / (1000 * 60 * 60 * 24)));
  const daysElapsed = Math.min(totalDays, Math.max(0, totalDays - daysLeft));
  const progressPercent = totalDays > 0 ? Math.round((daysElapsed / totalDays) * 100) : 0;

  const visibleParticipants = showAllParticipants
    ? (participants ?? [])
    : (participants ?? []).slice(0, PARTICIPANT_PREVIEW_COUNT);

  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setLinkCopied(true);
      setTimeout(() => setLinkCopied(false), 2000);
    } catch {
      // 클립보드 권한이 없는 환경(권한 거부 등)에서는 조용히 무시
    }
  };

  // 목록 페이지의 카테고리/상태/정렬 필터는 URL 쿼리로 관리되므로, 브라우저 뒤로가기처럼
  // 같은 히스토리로 돌아가야 필터가 유지된다. document.referrer는 클라이언트 사이드 라우팅에서
  // 갱신되지 않아 신뢰할 수 없으므로, 앱 안에서 이동해 쌓인 히스토리가 있는지만 확인한다.
  // 히스토리가 없다면(공유 링크로 바로 진입 등) 필터 없는 기본 목록으로 이동한다.
  const handleBack = () => {
    if (typeof window !== "undefined" && window.history.length > 1) {
      router.back();
    } else {
      router.push("/challenges");
    }
  };

  return (
    <div className="mx-auto max-w-[1440px] space-y-6 pb-2">
      <div className="flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={handleBack}
          className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
        >
          <ArrowLeft size={16} />
          목록으로
        </button>

        <div className="flex shrink-0 items-center gap-2">
          <button
            type="button"
            onClick={handleShare}
            className={`flex items-center justify-center gap-1.5 whitespace-nowrap rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
              linkCopied
                ? "border-primary bg-primary-light text-primary"
                : "border-gray-300 text-gray-600 hover:bg-gray-50"
            }`}
          >
            <Share2 size={14} />
            {linkCopied ? "복사 완료" : "공유하기"}
          </button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <div className="flex flex-col gap-8 sm:flex-row">
            <div
              className={`relative aspect-video w-full shrink-0 overflow-hidden rounded-xl shadow-sm sm:w-[38rem] ${
                challenge.thumbnailUrl ? "" : "bg-gray-50"
              }`}
            >
              {challenge.thumbnailUrl ? (
                <img src={challenge.thumbnailUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full items-center justify-center">
                  <img src="/logo.png" alt="" className="h-12 w-auto opacity-30" />
                </div>
              )}
              {isHost && (
                <>
                  <input
                    ref={thumbnailInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) thumbnailMutation.mutate({ challengeId, file });
                      e.target.value = "";
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => thumbnailInputRef.current?.click()}
                    disabled={thumbnailMutation.isPending}
                    className="absolute bottom-2 right-2 flex items-center gap-1.5 rounded-lg bg-black/60 px-2.5 py-1.5 text-xs font-medium text-white backdrop-blur-sm transition-colors hover:bg-black/70 disabled:opacity-50"
                  >
                    <Camera size={14} />
                    {thumbnailMutation.isPending ? "업로드 중..." : "사진 변경"}
                  </button>
                </>
              )}
            </div>

            <div className="flex flex-col">
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

              <h1 className="mt-3 text-2xl font-bold text-gray-900">{challenge.title}</h1>

              <div className="mt-4 flex w-fit items-center gap-2">
                <img
                  src={hostProfile?.profileImageUrl || DEFAULT_PROFILE_IMAGE_URL}
                  alt={`${challenge.hostNickname} 프로필 이미지`}
                  className="h-7 w-7 rounded-full border border-gray-200 object-cover"
                />
                <span className="text-sm font-medium text-gray-700">{challenge.hostNickname}</span>
                {hostProfile?.verified && (
                  <span className="flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-600">
                    <Award size={12} />
                    우수 방장
                  </span>
                )}
                {isHost && (
                  <span className="rounded-full bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">
                    방장
                  </span>
                )}
              </div>
            </div>
          </div>

          {thumbnailMutation.isError && <ErrorMessage error={thumbnailMutation.error} />}
          {thumbnailUploadFailed && (
            <p role="alert" className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-700">
              챌린지는 생성됐지만 사진 업로드에 실패했어요. 사진 변경 버튼으로 다시 시도해주세요.
            </p>
          )}

          <div className="grid grid-cols-2 gap-x-4 gap-y-5 rounded-2xl border border-gray-200 bg-surface p-5 shadow-sm sm:grid-cols-3">
            <InfoRow
              icon={Wallet}
              label="참가비"
              value={`₩ ${challenge.entryFee.toLocaleString()}`}
            />
            <InfoRow
              icon={Calendar}
              label="진행 기간"
              value={formatChallengePeriod(challenge.startDate, challenge.endDate)}
            />
            <InfoRow icon={Users} label="모집 정원" value={`${challenge.maxParticipants}명`} />
            <InfoRow
              icon={Users}
              label="현재 참가자"
              value={`${challenge.currentParticipants}명 (${Math.round(
                (challenge.currentParticipants / challenge.maxParticipants) * 100
              )}%)`}
            />
            <InfoRow
              icon={Target}
              label="성공 기준"
              value={`${challenge.successCriteriaRate}% 이상 달성 시 성공`}
            />
            <InfoRow
              icon={Bot}
              label="AI 자동검수"
              value={challenge.aiReviewEnabled ? "사용" : "미사용"}
            />
          </div>

          <div className="rounded-2xl border border-gray-200 bg-surface p-5 shadow-sm">
            <h2 className="text-sm font-bold text-gray-900">챌린지 소개</h2>
            <p
              ref={descriptionRef}
              className={`mt-2 text-sm leading-relaxed text-gray-600 whitespace-pre-line ${
                showFullDescription ? "" : "line-clamp-3"
              }`}
            >
              {challenge.description}
            </p>
            {isDescriptionClampable && (
              <button
                type="button"
                onClick={() => setShowFullDescription((prev) => !prev)}
                className="mt-3 flex w-full items-center justify-center gap-1 text-sm font-medium text-gray-500 hover:text-gray-700"
              >
                {showFullDescription ? "접기" : "더보기"}
                {showFullDescription ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </button>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div className="space-y-4 rounded-2xl border border-gray-200 bg-surface p-5 shadow-sm">
            <div>
              <p className="text-xs text-gray-400">참가비</p>
              <p className="mt-0.5 text-xl font-bold text-gray-900">
                ₩ {challenge.entryFee.toLocaleString()}
              </p>
            </div>

            {isParticipant ? (
              <div className="grid grid-cols-2 gap-3 rounded-lg bg-gray-50 px-3 py-2.5 text-sm">
                <div>
                  <p className="text-xs text-gray-400">남은 기간</p>
                  <p className="mt-0.5 font-semibold text-gray-800">{daysLeft}일</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">진행률</p>
                  <p className="mt-0.5 font-semibold text-gray-800">{progressPercent}%</p>
                </div>
              </div>
            ) : (
              me && (
                <div className="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2.5 text-sm">
                  <span className="text-gray-500">보유 캐시</span>
                  <span className="flex items-center gap-2 font-medium text-gray-700">
                    ₩ {(wallet?.availableBalance ?? 0).toLocaleString()}
                    <Link href="/wallet/charge" className="text-xs font-semibold text-primary">
                      충전하기
                    </Link>
                  </span>
                </div>
              )
            )}

            {canUseCertification && (
              <div className="space-y-2">
                <Link
                  href={`/challenges/${challengeId}/certification-posts/new`}
                  className="block w-full rounded-lg bg-primary py-2.5 text-center text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-hover"
                >
                  인증하기
                </Link>
                <Link
                  href={`/challenges/${challengeId}/feed`}
                  className="block w-full rounded-lg border border-primary py-2.5 text-center text-sm font-semibold text-primary transition-colors hover:bg-primary-light"
                >
                  챌린지 피드
                </Link>
              </div>
            )}

            <div className="space-y-2 border-t border-gray-100 pt-4">
              {isParticipant && isHost ? (
                <Link
                  href={`/challenges/${challengeId}/manage`}
                  className="block w-full rounded-lg border border-gray-300 py-2.5 text-center text-sm font-semibold text-gray-600 transition-colors hover:bg-gray-50"
                >
                  챌린지 관리
                </Link>
              ) : (
                <>
                  {isParticipant ? (
                    <button
                      type="button"
                      onClick={() => {
                        if (
                          window.confirm(
                            "정말 나가시겠어요? 나가면 이 챌린지에 다시 참가할 수 없어요."
                          )
                        ) {
                          leaveMutation.mutate();
                        }
                      }}
                      disabled={leaveMutation.isPending}
                      className="w-full rounded-lg border border-gray-300 py-2.5 text-sm font-semibold text-gray-600 transition-colors hover:bg-gray-50 disabled:opacity-50"
                    >
                      {leaveMutation.isPending ? "나가는 중..." : "참가 취소하기"}
                    </button>
                  ) : !me ? (
                    <Link
                      href={`/login?redirect=/challenges/${challengeId}`}
                      className="block w-full rounded-lg bg-primary py-2.5 text-center text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-hover"
                    >
                      로그인하고 참가하기
                    </Link>
                  ) : challenge.status !== "RECRUITING" ? (
                    <p className="text-center text-sm font-medium text-gray-400">
                      모집이 마감된 챌린지예요
                    </p>
                  ) : isFull ? (
                    <p className="text-center text-sm font-medium text-gray-400">
                      모집 정원이 가득 찼어요
                    </p>
                  ) : (
                    <>
                      <button
                        type="button"
                        onClick={() => joinMutation.mutate()}
                        disabled={joinMutation.isPending || isWalletLoading || !hasEnoughCash}
                        className="w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-hover disabled:opacity-50"
                      >
                        {joinMutation.isPending
                          ? "참가하는 중..."
                          : isWalletLoading
                            ? "잔액 확인 중..."
                            : "참가하기"}
                      </button>
                      {!isWalletLoading && !hasEnoughCash && (
                        <p className="text-center text-xs text-danger">
                          보유 캐시가 부족해요. 충전 후 참가할 수 있어요.
                        </p>
                      )}
                    </>
                  )}

                  {joinMutation.isError && <ErrorMessage error={joinMutation.error} />}
                  {leaveMutation.isError && <ErrorMessage error={leaveMutation.error} />}
                </>
              )}
            </div>

            {!isParticipant && (
              <p className="text-center text-xs text-gray-400">
                참가비는 참가 시점에 보유 캐시에서 보관돼요.
              </p>
            )}
          </div>

          <div className="rounded-2xl border border-gray-200 bg-surface p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-gray-900">
                참여자 {participants?.length ?? 0}
              </h2>
              {(participants?.length ?? 0) > PARTICIPANT_PREVIEW_COUNT && (
                <button
                  type="button"
                  onClick={() => setShowAllParticipants((prev) => !prev)}
                  className="text-xs font-semibold text-gray-400 hover:text-gray-600"
                >
                  {showAllParticipants ? "접기" : "더보기"}
                </button>
              )}
            </div>
            <ul className="mt-3 space-y-1">
              {visibleParticipants.map((participant) => (
                <li
                  key={participant.userId}
                  className="flex items-center justify-between rounded-lg px-2 py-1.5 text-sm text-gray-600 hover:bg-gray-50"
                >
                  <span>{participant.nickname}</span>
                  {participant.userId === challenge.hostId && (
                    <span className="rounded-full bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">
                      방장
                    </span>
                  )}
                </li>
              ))}
              {(participants?.length ?? 0) === 0 && (
                <li className="text-sm text-gray-400">아직 참가자가 없어요.</li>
              )}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChallengeDetailPage;
