"use client";

import Link from "next/link";
import Image from "next/image";
import { useChallenges } from "@/features/challenge/hooks";
import ChallengeCard from "@/features/challenge/ChallengeCard";
import {
  useMyMaxCertificationStreak,
  usePopularFeed,
  useTodayCertificationProgress,
} from "./hooks";
import ProfileAvatar from "@/components/ui/ProfileAvatar";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";
import { formatRelativeTime } from "@/lib/date";
import {
  ArrowRight,
  ClipboardCheck,
  CheckCircle2,
  CircleHelp,
  FileText,
  Flame,
  Gift,
  Heart,
  MessageCircle,
  ScanSearch,
  ShieldCheck,
  UsersRound,
} from "lucide-react";

const CertificationStatCard = ({ icon: Icon, iconClassName, label, value, badge }) => (
  <div className="relative rounded-2xl border border-gray-200 bg-white p-2">
    <span className={`flex h-6 w-6 items-center justify-center rounded-full ${iconClassName}`}>
      <Icon size={14} strokeWidth={2.5} aria-hidden="true" />
    </span>
    {badge != null && (
      <span className="absolute right-2 top-2 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-[11px] font-bold text-white">
        {badge}
      </span>
    )}
    <p className="mt-1 text-xs font-medium text-gray-500">{label}</p>
    <strong className="mt-0 block text-base font-bold text-gray-900">{value}</strong>
  </div>
);

const PopularFeedItem = ({ feed }) => (
  <article className="flex gap-3 py-2.5 first:pt-0 last:pb-0">
    <ProfileAvatar nickname={feed.authorNickname} profileImageUrl={feed.authorProfileImageUrl} />
    <div className="min-w-0 flex-1">
      <p className="text-sm font-bold text-gray-800">
        {feed.authorNickname}
        <span className="ml-2 text-xs font-normal text-gray-400">
          · {formatRelativeTime(feed.createdAt)}
        </span>
      </p>
      <p className="mt-1 line-clamp-2 text-xs leading-5 text-gray-600">{feed.contents}</p>
      <div className="mt-2 flex items-center gap-4 text-xs text-gray-400">
        <span className="flex items-center gap-1">
          <Heart size={14} aria-hidden="true" /> {feed.likeCount}
        </span>
        <span className="flex items-center gap-1">
          <MessageCircle size={14} aria-hidden="true" /> {feed.commentCount}
        </span>
      </div>
    </div>
  </article>
);

const homeFeatures = [
  {
    icon: ShieldCheck,
    title: "검증된 방장",
    description: "엄격한 기준으로 검증된 방장이 챌린지를 운영해요.",
  },
  {
    icon: ScanSearch,
    title: "AI 인증 검수",
    description: "AI가 인증을 꼼꼼하게 검수하여 공정한 참여를 보장해요.",
  },
  {
    icon: Gift,
    title: "성공 시 리워드",
    description: "목표 달성 시 참가비와 리워드를 100% 돌려드려요.",
  },
  {
    icon: UsersRound,
    title: "함께 성장하는 커뮤니티",
    description: "서로 응원하고 동기부여 받으며 함께 성장해요.",
  },
];

const HomePage = () => {
  const {
    data: challengesData,
    isLoading: isChallengesLoading,
    isError: isChallengesError,
    error: challengesError,
  } = useChallenges({ size: 4, status: "RECRUITING" });
  const {
    data: popularFeed,
    isLoading: isFeedLoading,
    isError: isFeedError,
    error: feedError,
  } = usePopularFeed();
  const {
    data: maxCertificationStreak,
    isLoading: isStreakLoading,
    isError: isStreakError,
  } = useMyMaxCertificationStreak();
  const {
    data: todayCertificationProgress,
    isLoading: isTodayCertificationLoading,
    isError: isTodayCertificationError,
  } = useTodayCertificationProgress();

  const challenges = challengesData?.content ?? [];
  const popularFeeds = popularFeed ?? [];
  const isTodayCertificationUnavailable = isTodayCertificationLoading || isTodayCertificationError;
  const todayCertificationValue = isTodayCertificationLoading
    ? "불러오는 중"
    : isTodayCertificationError
      ? "조회 실패"
      : `${todayCertificationProgress?.certifiedChallengeCount ?? 0}/${todayCertificationProgress?.inProgressChallengeCount ?? 0}`;
  const hostedTodayCertificationValue = isTodayCertificationUnavailable
    ? isTodayCertificationLoading
      ? "불러오는 중"
      : "조회 실패"
    : `${todayCertificationProgress?.hostedTodayCertificationPostCount ?? 0}건`;
  const hostedPendingCertificationCount =
    todayCertificationProgress?.hostedPendingCertificationPostCount ?? 0;
  const hostedPendingCertificationValue = isTodayCertificationUnavailable
    ? isTodayCertificationLoading
      ? "불러오는 중"
      : "조회 실패"
    : `${hostedPendingCertificationCount}건`;

  // 표시 건수와 동일한 today-progress 응답 스냅샷으로 이동 대상을 결정한다.
  // 미검수 인증이 있는 IN_PROGRESS 운영 챌린지가 딱 하나면 백엔드가 그 챌린지 ID를 내려주고
  // (→ 인증 관리로 직행), 여러 개거나 없으면 null(→ 내 챌린지 운영 중 목록에서 직접 고르게).
  const pendingReviewChallengeId = todayCertificationProgress?.hostedPendingCertificationChallengeId;
  const pendingReviewHref = pendingReviewChallengeId
    ? `/challenges/${pendingReviewChallengeId}/manage?tab=certifications`
    : "/my-challenges?tab=hosting";

  return (
    <div className="mx-auto max-w-[1440px] space-y-4 pb-2">
      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.9fr)_minmax(300px,0.85fr)]">
        <div className="relative overflow-hidden rounded-3xl">
          <Image
            src="/home-hero-banner.png"
            alt="오늘의 인증이 내일의 보상이 됩니다"
            width={1916}
            height={821}
            priority
            sizes="(min-width: 1536px) 950px, (min-width: 1280px) 62vw, 100vw"
            className="h-auto w-full"
          />
          <Link
            href="/challenges"
            className="absolute bottom-[7%] left-[7%] inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-bold text-white shadow-sm transition-colors hover:bg-primary-hover sm:px-4 sm:py-2"
          >
            챌린지 둘러보기 <ArrowRight size={14} aria-hidden="true" />
          </Link>
        </div>

        <aside className="rounded-3xl border border-gray-200 bg-surface p-3">
          <div className="flex items-center gap-1.5">
            <h2 className="text-base font-bold text-gray-900">오늘의 인증 현황</h2>
            <CircleHelp size={18} className="text-gray-400" aria-label="오늘의 인증 현황 안내" />
          </div>

          <section className="mt-2">
            <h3 className="text-sm font-bold text-primary">나의 인증</h3>
            <div className="mt-1 grid grid-cols-2 gap-2">
              <CertificationStatCard
                icon={CheckCircle2}
                iconClassName="bg-blue-50 text-primary"
                label="오늘 인증"
                value={todayCertificationValue}
              />
              <CertificationStatCard
                icon={Flame}
                iconClassName="bg-orange-50 text-orange-500"
                label="연속 성공일"
                value={
                  isStreakLoading
                    ? "불러오는 중"
                    : isStreakError
                      ? "조회 실패"
                      : `${maxCertificationStreak ?? 0}일`
                }
              />
            </div>
          </section>

          <section className="mt-1.5 border-t border-gray-100 pt-1.5">
            <h3 className="text-sm font-bold text-primary">운영 챌린지</h3>
            <div className="mt-1 grid grid-cols-2 gap-2">
              <CertificationStatCard
                icon={FileText}
                iconClassName="bg-emerald-50 text-emerald-500"
                label="오늘 올라온 인증"
                value={hostedTodayCertificationValue}
              />
              <CertificationStatCard
                icon={ClipboardCheck}
                iconClassName="bg-orange-50 text-orange-500"
                label="미검수 인증"
                value={hostedPendingCertificationValue}
                badge={!isTodayCertificationUnavailable && hostedPendingCertificationCount > 0 ? hostedPendingCertificationCount : null}
              />
            </div>
          </section>
          <Link
            href={pendingReviewHref}
            className="mt-2 flex items-center justify-center gap-1.5 rounded-xl bg-primary px-4 py-1.5 text-sm font-bold text-white transition-colors hover:bg-primary-hover"
          >
            {isTodayCertificationUnavailable
              ? "미검수 인증 확인하기"
              : `미검수 인증 ${hostedPendingCertificationCount}건 확인하기`}
            <ArrowRight size={16} aria-hidden="true" />
          </Link>
        </aside>
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.9fr)_minmax(300px,0.85fr)]">
        <section className="rounded-2xl border border-gray-200 bg-surface p-4">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-base font-bold text-gray-900">추천 챌린지</h2>
            <Link
              href="/challenges"
              className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-primary"
            >
              더보기 <ArrowRight size={15} aria-hidden="true" />
            </Link>
          </div>
          {isChallengesLoading ? (
            <Loading label="추천 챌린지 불러오는 중..." />
          ) : isChallengesError ? (
            <ErrorMessage error={challengesError} />
          ) : challenges.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-400">추천 챌린지가 없습니다.</p>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              {challenges.map((challenge) => (
                <ChallengeCard key={challenge.id} challenge={challenge} />
              ))}
            </div>
          )}
        </section>

        <section className="rounded-2xl border border-gray-200 bg-surface p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <h2 className="text-base font-bold text-gray-900">인기 피드</h2>
            <Link
              href="/feeds"
              className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-primary"
            >
              더보기 <ArrowRight size={15} aria-hidden="true" />
            </Link>
          </div>
          {isFeedLoading ? (
            <Loading label="인기 피드 불러오는 중..." />
          ) : isFeedError ? (
            <ErrorMessage error={feedError} />
          ) : popularFeeds.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-400">인기 피드가 없습니다.</p>
          ) : (
            <div className="divide-y divide-gray-100">
              {popularFeeds.map((feed) => (
                <PopularFeedItem key={feed.postId} feed={feed} />
              ))}
            </div>
          )}
        </section>
      </section>

      <section className="grid gap-4 rounded-2xl border border-gray-200 bg-surface p-4 sm:grid-cols-2 xl:grid-cols-4">
        {homeFeatures.map(({ icon: Icon, title, description }) => (
          <div
            key={title}
            className="flex gap-3 xl:not-last:border-r xl:not-last:border-gray-200 xl:pr-4"
          >
            <Icon
              className="shrink-0 text-primary"
              size={38}
              strokeWidth={1.8}
              aria-hidden="true"
            />
            <div>
              <h2 className="text-sm font-bold text-gray-900">{title}</h2>
              <p className="mt-1 text-xs leading-5 text-gray-500">{description}</p>
            </div>
          </div>
        ))}
      </section>

      <footer className="mt-10 border-t border-gray-200 pt-6 pb-4 text-center text-xs text-gray-400">
        <p className="font-semibold text-gray-500">Proovy</p>
        <p className="mt-1">습관인증 챌린지 서비스 · 함께 만들어가는 갓생</p>
        <p className="mt-2">© {new Date().getFullYear()} Proovy. All rights reserved.</p>
      </footer>
    </div>
  );
};

export default HomePage;
