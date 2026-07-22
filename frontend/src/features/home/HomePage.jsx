"use client";

import Link from "next/link";
import Image from "next/image";
import { useWallet } from "@/features/wallet/hooks";
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
  CheckCircle2,
  CircleDollarSign,
  Flame,
  Gift,
  Heart,
  MessageCircle,
  ScanSearch,
  ShieldCheck,
  UsersRound,
} from "lucide-react";

const StatRow = ({ icon: Icon, iconClassName, label, value }) => (
  <div className="flex items-center gap-3 border-b border-gray-100 py-3 last:border-b-0">
    <span
      className={`flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 ${iconClassName}`}
    >
      <Icon size={19} strokeWidth={2.5} aria-hidden="true" />
    </span>
    <span className="text-sm font-medium text-gray-700">{label}</span>
    <strong className="ml-auto text-sm text-gray-900">{value}</strong>
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
    data: wallet,
    isLoading: isWalletLoading,
    isError: isWalletError,
    error: walletError,
  } = useWallet();
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
  const isWalletUnauthenticated = isWalletError && walletError?.code === "C004";

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

        <aside className="rounded-3xl border border-gray-200 bg-surface p-5">
          <h2 className="text-base font-bold text-gray-900">오늘의 인증 현황</h2>
          <div className="mt-2">
            <StatRow
              icon={CheckCircle2}
              iconClassName="bg-blue-50 text-primary"
              label="오늘 인증"
              value={
                isTodayCertificationLoading
                  ? "불러오는 중..."
                  : isTodayCertificationError
                    ? "조회 실패"
                    : `${todayCertificationProgress?.certifiedChallengeCount ?? 0}/${todayCertificationProgress?.inProgressChallengeCount ?? 0}`
              }
            />
            <StatRow
              icon={Flame}
              iconClassName="bg-orange-50 text-orange-500"
              label="연속 성공일"
              value={
                isStreakLoading
                  ? "불러오는 중..."
                  : isStreakError
                    ? "조회 실패"
                    : `${maxCertificationStreak ?? 0}일`
              }
            />
            {isWalletLoading ? (
              <Loading label="포인트 불러오는 중..." />
            ) : isWalletError && !isWalletUnauthenticated ? (
              <ErrorMessage error={walletError} />
            ) : (
              <StatRow
                icon={CircleDollarSign}
                iconClassName="bg-blue-50 text-primary"
                label="보유 포인트"
                value={`${(wallet?.availableBalance ?? 0).toLocaleString()}P`}
              />
            )}
          </div>
          <Link
            href="/wallet/charge"
            className="mt-4 flex items-center justify-center rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-white transition-colors hover:bg-primary-hover"
          >
            포인트 충전하기
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
