"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import Link from "next/link";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { DEFAULT_PROFILE_IMAGE_URL } from "@/lib/constants";

import { useFollowers, useFollowing } from "../hooks";

const TITLE = { followers: "팔로워", following: "팔로잉" };
const EMPTY_TEXT = { followers: "아직 팔로워가 없어요.", following: "아직 팔로우한 사람이 없어요." };

const FollowListPage = ({ userId, type }) => {
  const hook = type === "followers" ? useFollowers : useFollowing;
  const { data, error, fetchNextPage, hasNextPage, isError, isFetchingNextPage, isLoading } = hook(userId);

  const items = data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-4">
      <h1 className="text-xl font-bold text-gray-900">{TITLE[type]}</h1>

      {isLoading ? (
        <Loading />
      ) : isError ? (
        <ErrorMessage error={error} />
      ) : items.length === 0 ? (
        <Card>
          <p className="py-12 text-center text-sm text-gray-500">{EMPTY_TEXT[type]}</p>
        </Card>
      ) : (
        <>
          <div className="flex flex-col gap-2">
            {items.map((item) => (
              <Link
                key={item.userId}
                href={`/users/${item.userId}`}
                className="flex items-center gap-3 rounded-lg border border-gray-200 bg-surface px-4 py-3 hover:bg-gray-50"
              >
                <img
                  src={item.profileImageUrl || DEFAULT_PROFILE_IMAGE_URL}
                  alt={`${item.nickname} 프로필 이미지`}
                  className="h-10 w-10 shrink-0 rounded-full border border-gray-200 object-cover"
                />
                <span className="truncate text-sm font-medium text-gray-900">{item.nickname}</span>
              </Link>
            ))}
          </div>

          {hasNextPage && (
            <div className="flex justify-center">
              <Button variant="outline" onClick={() => fetchNextPage()} disabled={isFetchingNextPage}>
                {isFetchingNextPage ? "불러오는 중..." : "더 보기"}
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default FollowListPage;
