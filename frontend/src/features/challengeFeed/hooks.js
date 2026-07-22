"use client";

import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";

import { approveCertificationPost, getChallengeFeed, rejectCertificationPost } from "./api";

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

const invalidateReviewQueries = (queryClient, challengeId) =>
  queryClient.invalidateQueries({ queryKey: ["challenge-feed", challengeId] });

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
