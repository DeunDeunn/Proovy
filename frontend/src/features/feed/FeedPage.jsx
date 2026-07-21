"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import { BadgeCheck, Heart, ImageOff, MessageCircle } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

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

const FeedCard = ({ post }) => (
  <Card className="overflow-hidden p-0">
    <Link
      href={`/certification-posts/${post.postId}`}
      aria-label="인증 게시글 상세 보기"
      className="group block"
    >
      {post.thumbnailUrl ? (
        <img
          src={post.thumbnailUrl}
          alt={`${post.authorNickname ?? "사용자"}의 인증 이미지`}
          className="aspect-video w-full bg-gray-100 object-cover transition-transform duration-200 group-hover:scale-[1.02]"
        />
      ) : (
        <div className="flex aspect-video items-center justify-center bg-gray-100 text-gray-400 transition-colors group-hover:bg-gray-200">
          <ImageOff size={32} aria-hidden="true" />
          <span className="sr-only">인증 이미지 없음</span>
        </div>
      )}
    </Link>

    <div className="p-5">
      <div className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
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
            <p className="mt-1 text-xs text-gray-400">
              챌린지 #{post.challengeId} · {formatCreatedAt(post.createdAt)}
            </p>
          </div>
        </div>
      </div>

      <p className="mt-4 whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">
        {post.contents || "작성한 인증 내용이 없습니다."}
      </p>

      <div className="mt-5 flex items-center gap-4 border-t border-gray-100 pt-4 text-sm text-gray-500">
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

  const posts = data?.pages.flat() ?? [];

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">전체 인증 피드</h1>
        <p className="mt-2 text-sm text-gray-500">
          전체 공개 챌린지에서 승인된 인증글을 둘러보세요.
        </p>
      </div>

      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-lg bg-gray-100 p-1" role="tablist" aria-label="피드 필터">
          {filters.map((item) => {
            const isSelected = filter === item.value;
            return (
              <button
                key={item.value}
                type="button"
                role="tab"
                aria-selected={isSelected}
                onClick={() => setFilter(item.value)}
                className={`rounded-md px-4 py-2 text-sm font-medium transition-colors ${
                  isSelected
                    ? "bg-surface text-primary shadow-sm"
                    : "text-gray-500 hover:text-gray-700"
                }`}
              >
                {item.label}
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-2" aria-label="피드 정렬">
          <button
            type="button"
            className="rounded-lg border border-primary bg-primary-light px-3 py-2 text-sm font-semibold text-primary"
          >
            최신순
          </button>
          <button
            type="button"
            disabled
            title="인기순 정렬은 준비 중입니다."
            className="cursor-not-allowed rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm font-medium text-gray-400"
          >
            인기순 · 준비 중
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
              <FeedCard key={post.postId} post={post} />
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
