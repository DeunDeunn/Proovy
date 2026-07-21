"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import Link from "next/link";
import { Award, Heart, ImageOff, MessageCircle } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";
import { DEFAULT_PROFILE_IMAGE_URL } from "@/lib/constants";

import { useFollow, useUnfollow, useUserFeed, useUserProfile } from "../hooks";

const formatCreatedAt = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("ko-KR", { month: "long", day: "numeric" }).format(date);
};

const UserFeedCard = ({ post }) => (
  <Card className="self-start overflow-hidden !p-4">
    <Link href={`/certification-posts/${post.postId}`} aria-label="인증 게시글 상세 보기" className="group block">
      {post.thumbnailUrl ? (
        <img
          src={post.thumbnailUrl}
          alt="인증 이미지"
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
      <p className="text-xs text-gray-400">
        챌린지 #{post.challengeId} · {formatCreatedAt(post.createdAt)}
      </p>
      <p className="mt-2 line-clamp-2 whitespace-pre-wrap break-words text-sm leading-5 text-gray-700">
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

const UserProfilePage = ({ userId }) => {
  const { data: me, isLoading: isMeLoading } = useMe();
  const { data: profile, isLoading, isError, error } = useUserProfile(userId);
  const followMutation = useFollow(userId);
  const unfollowMutation = useUnfollow(userId);
  const {
    data: feedData,
    error: feedError,
    fetchNextPage,
    hasNextPage,
    isError: isFeedError,
    isFetchingNextPage,
    isLoading: isFeedLoading,
  } = useUserFeed(userId);

  if (isLoading) return <Loading />;
  if (isError) return <ErrorMessage error={error} />;
  if (!profile) return null;

  const isOwnProfile = me?.id != null && String(me.id) === String(userId);
  const posts = feedData?.pages.flat() ?? [];
  const followPending = followMutation.isPending || unfollowMutation.isPending;

  const handleFollowToggle = () => {
    if (profile.following) {
      unfollowMutation.mutate();
    } else {
      followMutation.mutate();
    }
  };

  return (
    <div className="mx-auto flex max-w-[1440px] flex-col gap-4">
      <div className="flex items-center gap-4">
        <img
          src={profile.profileImageUrl || DEFAULT_PROFILE_IMAGE_URL}
          alt={`${profile.nickname} 프로필 이미지`}
          className="h-16 w-16 rounded-full border border-gray-200 object-cover"
        />

        <div className="flex flex-1 flex-col gap-1">
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold text-gray-900">{profile.nickname}</span>
            {profile.verified && (
              <span className="flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-600">
                <Award size={12} />
                우수 사용자
              </span>
            )}
          </div>

          <div className="flex gap-3 text-sm text-gray-700">
            <Link href={`/users/${userId}/followers`} className="hover:underline">
              <strong className="font-semibold">{profile.followerCount}</strong> 팔로워
            </Link>
            <Link href={`/users/${userId}/following`} className="hover:underline">
              <strong className="font-semibold">{profile.followingCount}</strong> 팔로잉
            </Link>
          </div>
        </div>

        {isMeLoading ? null : isOwnProfile ? (
          <Link
            href="/mypage"
            className="cursor-pointer rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50"
          >
            내 마이페이지
          </Link>
        ) : (
          <Button
            variant={profile.following ? "outline" : "primary"}
            onClick={handleFollowToggle}
            disabled={followPending}
          >
            {profile.following ? "팔로잉" : "팔로우"}
          </Button>
        )}
      </div>

      {(followMutation.isError || unfollowMutation.isError) && (
        <ErrorMessage error={followMutation.error ?? unfollowMutation.error} />
      )}

      <div className="mt-2">
        <h2 className="mb-3 text-sm font-semibold text-gray-900">인증피드</h2>

        {isFeedLoading ? (
          <Loading label="인증글을 불러오는 중..." />
        ) : isFeedError ? (
          <ErrorMessage error={feedError} />
        ) : posts.length === 0 ? (
          <Card>
            <p className="py-12 text-center text-sm text-gray-500">아직 올린 인증글이 없어요.</p>
          </Card>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {posts.map((post) => (
                <UserFeedCard key={post.postId} post={post} />
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
    </div>
  );
};

export default UserProfilePage;
