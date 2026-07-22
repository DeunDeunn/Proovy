"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  followUser,
  getFollowers,
  getFollowing,
  getUserFeed,
  getUserProfile,
  unfollowUser,
} from "./api";

const LIST_PAGE_SIZE = 20;
const FEED_PAGE_SIZE = 20;

// 라우트 파라미터로 받은 userId(문자열)와 API 응답의 authorId(숫자)가 섞여서 호출되는데,
// react-query는 쿼리 키를 구조적으로 비교해 8과 "8"을 다른 키로 취급한다. 타입이 갈리면
// 팔로우/언팔로우 후 invalidateQueries가 엉뚱한(호출한 쪽과 같은 타입의) 캐시만 갱신하고,
// 다른 화면에 떠 있는 반대 타입의 프로필 쿼리는 갱신되지 않아 새로고침 전까지 낡은 값이 보인다.
// 그래서 이 파일의 모든 쿼리 키는 userId를 항상 String으로 통일해서 만든다.
export const useUserProfile = (userId) =>
  useQuery({
    queryKey: ["users", String(userId), "profile"],
    queryFn: () => getUserProfile(userId),
    enabled: !!userId,
  });

export const useFollow = (userId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => followUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users", String(userId), "profile"] });
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
    },
  });
};

export const useUnfollow = (userId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => unfollowUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users", String(userId), "profile"] });
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
    },
  });
};

export const useFollowers = (userId) =>
  useInfiniteQuery({
    queryKey: ["users", String(userId), "followers"],
    queryFn: ({ pageParam }) => getFollowers(userId, { page: pageParam, size: LIST_PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
    enabled: !!userId,
  });

export const useFollowing = (userId) =>
  useInfiniteQuery({
    queryKey: ["users", String(userId), "following"],
    queryFn: ({ pageParam }) => getFollowing(userId, { page: pageParam, size: LIST_PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
    enabled: !!userId,
  });

export const useUserFeed = (userId) =>
  useInfiniteQuery({
    queryKey: ["users", String(userId), "feed"],
    queryFn: ({ pageParam }) => getUserFeed(userId, { cursor: pageParam, size: FEED_PAGE_SIZE }),
    initialPageParam: null,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < FEED_PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.postId;
    },
    enabled: !!userId,
  });
