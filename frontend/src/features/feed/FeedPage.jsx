"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import {
  BadgeCheck,
  ChevronDown,
  Clock3,
  Heart,
  ImageOff,
  MessageCircle,
  MessageSquare,
} from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";
import { useStartDirectChat } from "@/features/chat/hooks/chatHooks";

import { usePublicFeed } from "./hooks";

import ProfileAvatar from "@/components/ui/ProfileAvatar";

const filters = [
  { value: "all", label: "전체" },
  { value: "mine", label: "내 인증" },
  { value: "today", label: "오늘" },
];

const formatCreatedAt = (value) => {
  if (!value) return "";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

const FeedCard = ({ post, currentUserId, onStartChat, isStartingChat }) => (
  <Card className="self-start overflow-hidden !p-4">
    <Link
      href={`/certification-posts/${post.postId}`}
      aria-label="인증 게시글 상세 보기"
      className="group block"
    >
      {post.thumbnailUrl ? (
        <img
          src={post.thumbnailUrl}
          alt={`${post.authorNickname ?? "사용자"}의 인증 이미지`}
          className="aspect-[1.6/1] w-full bg-gray-100 object-cover transition-transform duration-200 group-hover:scale-[1.02]"
        />
      ) : (
        <div className="flex aspect-[1.6/1] items-center justify-center bg-gray-100 text-gray-400 transition-colors group-hover:bg-gray-200">
          <ImageOff size={32} aria-hidden="true" />
          <span className="sr-only">인증 이미지 없음</span>
        </div>
      )}
    </Link>

    <div className="pt-3">
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2.5">
          <ProfileAvatar
            nickname={post.authorNickname}
            profileImageUrl={post.authorProfileImageUrl}
          />
          <div className="min-w-0">
            <div className="flex items-center gap-1.5">
              <p className="truncate text-sm font-semibold text-gray-900">
                {post.authorNickname ?? "알 수 없는 사용자"}
              </p>
              {post.authorBadgeApproved && (
                <span className="inline-flex shrink-0 text-sky-500" title="인증 회원">
                  <BadgeCheck size={16} aria-hidden="true" />
                  <span className="sr-only">인증 회원</span>
                </span>
              )}
            </div>
            <p className="mt-0.5 text-xs text-gray-400">
              챌린지 #{post.challengeId} · {formatCreatedAt(post.createdAt)}
            </p>
          </div>
        </div>
        {currentUserId != null && currentUserId !== post.authorId && (
          <button
            type="button"
            onClick={() => onStartChat(post.authorId)}
            disabled={isStartingChat}
            aria-label="채팅하기"
            title="채팅하기"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-900 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <MessageSquare size={17} aria-hidden="true" />
          </button>
        )}
      </div>

      <p className="mt-3 line-clamp-2 whitespace-pre-wrap break-words text-sm leading-5 text-gray-700">
        {post.contents || "작성한 인증 내용이 없습니다."}
      </p>

      <div className="mt-3 flex items-center gap-4 border-t border-gray-100 pt-3 text-sm text-gray-500">
        <span className="flex items-center gap-1.5">
          <Heart size={17} aria-hidden="true" />
          {Number(post.likeCount ?? 0).toLocaleString()}
        </span>
        <span className="flex items-center gap-1.5">
          <MessageCircle size={17} aria-hidden="true" />
          {Number(post.commentCount ?? 0).toLocaleString()}
        </span>
      </div>
    </div>
  </Card>
);

const FeedPage = () => {
  const [filter, setFilter] = useState("all");
  const { data, error, fetchNextPage, hasNextPage, isError, isFetchingNextPage, isLoading } =
    usePublicFeed(filter);
  const { data: me } = useMe();
  const { startChat, isPending: isStartingChat } = useStartDirectChat();

  const posts = data?.pages.flat() ?? [];

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="mb-5">
        <h1 className="text-2xl font-bold text-gray-900">전체 인증 피드</h1>
        <p className="mt-2 text-sm text-gray-500">
          전체 공개 챌린지에서 승인된 인증글을 둘러보세요.
        </p>
      </div>

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-3" role="tablist" aria-label="피드 필터">
          {filters.map((item) => {
            const isSelected = filter === item.value;
            return (
              <button
                key={item.value}
                type="button"
                role="tab"
                aria-selected={isSelected}
                onClick={() => setFilter(item.value)}
                className={`rounded-xl border px-4 py-2 text-sm font-semibold transition-colors ${
                  isSelected
                    ? "border-primary bg-primary text-white shadow-sm"
                    : "border-gray-200 bg-surface text-gray-600 hover:border-gray-300 hover:text-gray-800"
                }`}
              >
                {item.label}
              </button>
            );
          })}
        </div>

        <div aria-label="피드 정렬">
          <button
            type="button"
            title="현재 최신순으로 정렬되어 있습니다."
            className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-surface px-4 py-2 text-sm font-semibold text-gray-600 shadow-sm"
          >
            <Clock3 size={17} aria-hidden="true" />
            최신순
            <ChevronDown size={16} aria-hidden="true" />
          </button>
        </div>
      </div>

      {isLoading ? (
        <Loading label="인증글을 불러오는 중..." />
      ) : isError ? (
        <ErrorMessage error={error} />
      ) : posts.length === 0 ? (
        <Card>
          <p className="py-12 text-center text-sm text-gray-500">표시할 인증글이 없습니다.</p>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {posts.map((post) => (
              <FeedCard
                key={post.postId}
                post={post}
                currentUserId={me?.id}
                onStartChat={startChat}
                isStartingChat={isStartingChat}
              />
            ))}
          </div>

          {hasNextPage && (
            <div className="mt-6 flex justify-center">
              <Button
                type="button"
                variant="outline"
                onClick={() => fetchNextPage()}
                disabled={isFetchingNextPage}
              >
                {isFetchingNextPage ? "불러오는 중..." : "더 보기"}
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default FeedPage;
