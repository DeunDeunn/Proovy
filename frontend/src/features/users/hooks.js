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

export const useUserProfile = (userId) =>
  useQuery({
    queryKey: ["users", userId, "profile"],
    queryFn: () => getUserProfile(userId),
    enabled: !!userId,
  });

export const useFollow = (userId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => followUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users", userId, "profile"] });
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
    },
  });
};

export const useUnfollow = (userId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => unfollowUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users", userId, "profile"] });
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
    },
  });
};

export const useFollowers = (userId) =>
  useInfiniteQuery({
    queryKey: ["users", userId, "followers"],
    queryFn: ({ pageParam }) => getFollowers(userId, { page: pageParam, size: LIST_PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
    enabled: !!userId,
  });

export const useFollowing = (userId) =>
  useInfiniteQuery({
    queryKey: ["users", userId, "following"],
    queryFn: ({ pageParam }) => getFollowing(userId, { page: pageParam, size: LIST_PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
    enabled: !!userId,
  });

export const useUserFeed = (userId) =>
  useInfiniteQuery({
    queryKey: ["users", userId, "feed"],
    queryFn: ({ pageParam }) => getUserFeed(userId, { cursor: pageParam, size: FEED_PAGE_SIZE }),
    initialPageParam: null,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < FEED_PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.postId;
    },
    enabled: !!userId,
  });
