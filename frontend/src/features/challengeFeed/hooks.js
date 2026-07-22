"use client";

import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";

import {
  approveCertificationPost,
  getChallengeFeed,
  getPendingCertifications,
  rejectCertificationPost,
} from "./api";

const PAGE_SIZE = 20;

export const useChallengeFeed = (challengeId, filter, sort) =>
  useInfiniteQuery({
    queryKey: ["challenge-feed", challengeId, { filter, sort }],
    queryFn: ({ pageParam }) =>
      getChallengeFeed(challengeId, {
        cursor: pageParam?.cursor,
        cursorLike: pageParam?.cursorLike,
        filter,
        sort,
        size: PAGE_SIZE,
      }),
    initialPageParam: { cursor: null, cursorLike: null },
    enabled: !!challengeId,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < PAGE_SIZE) return undefined;
      const lastPost = lastPage[lastPage.length - 1];
      return {
        cursor: lastPost?.postId,
        cursorLike: sort === "popular" ? lastPost?.likeCount : null,
      };
    },
  });

export const usePendingCertifications = (challengeId, enabled) =>
  useInfiniteQuery({
    queryKey: ["pending-certifications", challengeId],
    queryFn: ({ pageParam }) =>
      getPendingCertifications(challengeId, { cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: null,
    enabled: Boolean(challengeId) && enabled,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.postId;
    },
  });

const invalidateReviewQueries = (queryClient, challengeId) =>
  Promise.all([
    queryClient.invalidateQueries({ queryKey: ["pending-certifications", challengeId] }),
    queryClient.invalidateQueries({ queryKey: ["challenge-feed", challengeId] }),
  ]);

export const useApproveCertificationPost = (challengeId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: approveCertificationPost,
    onSuccess: () => invalidateReviewQueries(queryClient, challengeId),
  });
};

export const useRejectCertificationPost = (challengeId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ postId, reason }) => rejectCertificationPost(postId, reason),
    onSuccess: () => invalidateReviewQueries(queryClient, challengeId),
  });
};
