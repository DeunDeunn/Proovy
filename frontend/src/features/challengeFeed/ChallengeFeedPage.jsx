"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import {
  BadgeCheck,
  Clock3,
  Flame,
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
import { useChallenge } from "@/features/challenge/hooks";
import { useStartDirectChat } from "@/features/chat/hooks/chatHooks";

import { useApproveCertificationPost, useChallengeFeed, useRejectCertificationPost } from "./hooks";

const filters = [
  { value: "all", label: "전체" },
  { value: "mine", label: "내 인증" },
  { value: "today", label: "오늘" },
];

const sorts = [
  { value: "latest", label: "최신순", icon: Clock3 },
  { value: "popular", label: "인기순", icon: Flame },
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

const getAvatarInitial = (nickname) => Array.from(nickname?.trim() || "?")[0];

const ProfileAvatar = ({ nickname, profileImageUrl }) =>
  profileImageUrl ? (
    <img
      src={profileImageUrl}
      alt={`${nickname ?? "사용자"} 프로필 이미지`}
      className="h-9 w-9 shrink-0 rounded-full border border-gray-200 object-cover"
    />
  ) : (
    <span
      aria-hidden="true"
      className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-light text-sm font-bold text-primary"
    >
      {getAvatarInitial(nickname)}
    </span>
  );

const ChallengeFeedCard = ({
  post,
  currentUserId,
  onStartChat,
  isStartingChat,
  startChatError,
  startChatTargetUserId,
}) => (
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
            <p className="mt-0.5 text-xs text-gray-400">{formatCreatedAt(post.createdAt)}</p>
          </div>
        </div>
        {currentUserId != null && currentUserId !== post.authorId && (
          <button
            type="button"
            onClick={() => onStartChat(post.authorId)}
            disabled={isStartingChat && startChatTargetUserId === post.authorId}
            aria-label="채팅하기"
            title="채팅하기"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-900 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <MessageSquare size={17} aria-hidden="true" />
          </button>
        )}
      </div>

      {startChatError && startChatTargetUserId === post.authorId && (
        <div className="mt-2">
          <ErrorMessage error={startChatError} />
        </div>
      )}

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

const ReviewCertificationCard = ({
  post,
  isRejecting,
  rejectReason,
  isActionPending,
  onApprove,
  onStartReject,
  onCancelReject,
  onRejectReasonChange,
  onReject,
}) => {
  const isPending = post.status === "PENDING";
  const isRejected = post.status === "REJECTED";

  return (
    <Card className="overflow-hidden p-0">
      <Link
        href={`/certification-posts/${post.postId}`}
        aria-label="검수 대상 인증글 상세 보기"
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
        <div className="flex min-w-0 items-center justify-between gap-3">
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
              <p className="mt-1 text-xs text-gray-400">{formatCreatedAt(post.createdAt)}</p>
            </div>
          </div>
          {isPending ? (
            <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">
              승인 대기
            </span>
          ) : (
            <span className="rounded-full bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700">
              {isRejected ? "반려" : "검수 상태 확인 필요"}
            </span>
          )}
        </div>

        <p className="mt-4 whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">
          {post.contents || "작성한 인증 내용이 없습니다."}
        </p>

        {isRejected && (
          <div className="mt-5 rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700">
            <span className="font-semibold">반려 사유: </span>
            {post.rejectionReason || "등록된 반려 사유가 없습니다."}
          </div>
        )}

        {isPending && (
          <div className="mt-5 border-t border-gray-100 pt-4">
            {isRejecting ? (
              <div>
                <label
                  htmlFor={`reject-reason-${post.postId}`}
                  className="text-sm font-medium text-gray-800"
                >
                  반려 사유
                </label>
                <textarea
                  id={`reject-reason-${post.postId}`}
                  value={rejectReason}
                  onChange={(event) => onRejectReasonChange(event.target.value)}
                  placeholder="반려 사유를 작성해주세요."
                  rows={3}
                  disabled={isActionPending}
                  className="mt-2 w-full resize-y rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-gray-50"
                />
                <div className="mt-3 flex justify-end gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={onCancelReject}
                    disabled={isActionPending}
                  >
                    취소
                  </Button>
                  <Button
                    type="button"
                    variant="danger"
                    onClick={() => onReject(post.postId)}
                    disabled={isActionPending || !rejectReason.trim()}
                  >
                    {isActionPending ? "처리 중..." : "반려 확정"}
                  </Button>
                </div>
              </div>
            ) : (
              <div className="flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => onStartReject(post.postId)}
                  disabled={isActionPending}
                >
                  반려
                </Button>
                <Button
                  type="button"
                  onClick={() => onApprove(post.postId)}
                  disabled={isActionPending}
                >
                  {isActionPending ? "처리 중..." : "승인"}
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </Card>
  );
};

const ChallengeFeedPage = ({ challengeId }) => {
  const [filter, setFilter] = useState("all");
  const [sort, setSort] = useState("latest");
  const [isReviewMode, setIsReviewMode] = useState(false);
  const [rejectTargetId, setRejectTargetId] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [reviewActionError, setReviewActionError] = useState(null);
  const { data: me } = useMe();
  const {
    startChat,
    isPending: isStartingChat,
    error: startChatError,
    targetUserId: startChatTargetUserId,
  } = useStartDirectChat();
  const {
    data: challenge,
    error: challengeError,
    isLoading: isChallengeLoading,
  } = useChallenge(challengeId);
  const isHost = me?.id != null && challenge?.hostId != null && me.id === challenge.hostId;
  const canReview = isHost || me?.role === "ADMIN";
  const activeFilter = isReviewMode ? "review" : filter;
  const activeSort = isReviewMode ? "latest" : sort;
  const {
    data,
    error: feedError,
    fetchNextPage,
    hasNextPage,
    isError: isFeedError,
    isFetchingNextPage,
    isLoading: isFeedLoading,
  } = useChallengeFeed(challengeId, activeFilter, activeSort);
  const approveMutation = useApproveCertificationPost(challengeId);
  const rejectMutation = useRejectCertificationPost(challengeId);

  const posts = data?.pages.flat() ?? [];
  const isReviewActionPending = approveMutation.isPending || rejectMutation.isPending;

  const resetRejectForm = () => {
    setRejectTargetId(null);
    setRejectReason("");
  };

  const toggleReviewMode = () => {
    if (isReviewActionPending) return;

    setIsReviewMode((current) => !current);
    resetRejectForm();
    setReviewActionError(null);
  };

  const handleApprove = (postId) => {
    if (isReviewActionPending) return;

    setReviewActionError(null);
    approveMutation.mutate(postId, {
      onSuccess: resetRejectForm,
      onError: setReviewActionError,
    });
  };

  const startReject = (postId) => {
    if (isReviewActionPending) return;

    setRejectTargetId(postId);
    setRejectReason("");
    setReviewActionError(null);
  };

  const handleReject = (postId) => {
    const reason = rejectReason.trim();
    if (!reason) {
      setReviewActionError({ message: "반려 사유를 입력해주세요." });
      return;
    }

    setReviewActionError(null);
    rejectMutation.mutate(
      { postId, reason },
      {
        onSuccess: resetRejectForm,
        onError: setReviewActionError,
      }
    );
  };

  if (isChallengeLoading || isFeedLoading) {
    return (
      <Loading
        label={isReviewMode ? "검수 대기 인증글을 불러오는 중..." : "챌린지 피드를 불러오는 중..."}
      />
    );
  }
  if (challengeError) return <ErrorMessage error={challengeError} />;

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="mb-5">
        <p className="text-sm font-medium text-primary">
          {challenge?.title ?? `챌린지 #${challengeId}`}
        </p>
        <h1 className="mt-1 text-2xl font-bold text-gray-900">
          {isReviewMode ? "인증글 검수" : "인증 피드"}
        </h1>
        <p className="mt-2 text-sm text-gray-500">
          {isReviewMode
            ? "승인 대기 인증글을 오래된 순으로 확인하고 처리하세요."
            : "참가자들이 승인받은 인증글을 확인해보세요."}
        </p>
      </div>

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        {isReviewMode ? (
          <p className="text-sm font-medium text-amber-700">승인 대기 인증글 · 오래된 순</p>
        ) : (
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
        )}

        <div className="flex items-center gap-2" aria-label="피드 정렬">
          {!isReviewMode && (
            <Link
              href={`/challenges/${challengeId}/certification-posts/new`}
              className="inline-flex items-center rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-hover"
            >
              + 인증하기
            </Link>
          )}
          {canReview && (
            <Button
              type="button"
              variant={isReviewMode ? "primary" : "outline"}
              onClick={toggleReviewMode}
              disabled={isReviewActionPending}
            >
              {isReviewMode ? "피드 보기" : "검수 모드"}
            </Button>
          )}
          {!isReviewMode && (
            <div className="flex gap-2" role="tablist" aria-label="피드 정렬">
              {sorts.map(({ value, label, icon: Icon }) => {
                const isSelected = sort === value;
                return (
                  <button
                    key={value}
                    type="button"
                    role="tab"
                    aria-selected={isSelected}
                    onClick={() => setSort(value)}
                    className={`inline-flex items-center gap-2 rounded-xl border px-4 py-2 text-sm font-semibold transition-colors ${
                      isSelected
                        ? "border-primary bg-primary text-white shadow-sm"
                        : "border-gray-200 bg-surface text-gray-600 hover:border-gray-300 hover:text-gray-800"
                    }`}
                  >
                    <Icon size={17} aria-hidden="true" />
                    {label}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {isReviewMode ? (
        <>
          {reviewActionError && (
            <div className="mb-4">
              <ErrorMessage error={reviewActionError} />
            </div>
          )}
          {isFeedError ? (
            <ErrorMessage error={feedError} />
          ) : posts.length === 0 ? (
            <Card>
              <p className="py-12 text-center text-sm text-gray-500">
                승인 대기 인증글이 없습니다.
              </p>
            </Card>
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {posts.map((post) => (
                  <ReviewCertificationCard
                    key={post.postId}
                    post={post}
                    isRejecting={rejectTargetId === post.postId}
                    rejectReason={rejectReason}
                    isActionPending={isReviewActionPending}
                    onApprove={handleApprove}
                    onStartReject={startReject}
                    onCancelReject={resetRejectForm}
                    onRejectReasonChange={setRejectReason}
                    onReject={handleReject}
                  />
                ))}
              </div>

              {hasNextPage && (
                <div className="mt-6 flex justify-center">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => fetchNextPage()}
                    disabled={isFetchingNextPage || isReviewActionPending}
                  >
                    {isFetchingNextPage ? "불러오는 중..." : "더 보기"}
                  </Button>
                </div>
              )}
            </>
          )}
        </>
      ) : isFeedError ? (
        <ErrorMessage error={feedError} />
      ) : posts.length === 0 ? (
        <Card>
          <p className="py-12 text-center text-sm text-gray-500">표시할 인증글이 없습니다.</p>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {posts.map((post) => (
              <ChallengeFeedCard
                key={post.postId}
                post={post}
                currentUserId={me?.id}
                onStartChat={startChat}
                isStartingChat={isStartingChat}
                startChatError={startChatError}
                startChatTargetUserId={startChatTargetUserId}
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

export default ChallengeFeedPage;
