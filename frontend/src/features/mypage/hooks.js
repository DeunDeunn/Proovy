"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { applyVerification, getMyFeed, getMyPage, getVerificationStatus } from "./api";

const FEED_PAGE_SIZE = 20;

export const useMyPage = () =>
  useQuery({
    queryKey: ["mypage"],
    queryFn: getMyPage,
  });

export const useVerificationStatus = () =>
  useQuery({
    queryKey: ["mypage", "verification-status"],
    queryFn: getVerificationStatus,
  });

export const useApplyVerification = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: applyVerification,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
    },
  });
};

export const useMyFeed = () =>
  useInfiniteQuery({
    queryKey: ["mypage", "feed"],
    queryFn: ({ pageParam }) => getMyFeed({ cursor: pageParam, size: FEED_PAGE_SIZE }),
    initialPageParam: null,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < FEED_PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.postId;
    },
  });
