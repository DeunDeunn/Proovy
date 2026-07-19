"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  createCertificationPost,
  createComment,
  getCertificationPost,
  getComments,
  updateCertificationPost,
} from "./api";

const COMMENT_PAGE_SIZE = 20;

export const useCertificationPost = (postId) =>
  useQuery({
    queryKey: ["certification-post", postId],
    queryFn: () => getCertificationPost(postId),
    enabled: !!postId,
  });

export const useCreateCertificationPost = (challengeId) =>
  useMutation({
    mutationFn: (payload) => createCertificationPost(challengeId, payload),
  });

export const useUpdateCertificationPost = (postId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload) => updateCertificationPost(postId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["certification-post", postId] });
      queryClient.invalidateQueries({ queryKey: ["feed"] });
      queryClient.invalidateQueries({ queryKey: ["challenge-feed"] });
    },
  });
};

export const useComments = (postId) =>
  useInfiniteQuery({
    queryKey: ["certification-comments", postId],
    queryFn: ({ pageParam }) => getComments(postId, { cursor: pageParam, size: COMMENT_PAGE_SIZE }),
    initialPageParam: null,
    enabled: !!postId,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < COMMENT_PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.commentId;
    },
  });

export const useCreateComment = (postId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload) => createComment(postId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["certification-comments", postId] });
      queryClient.invalidateQueries({ queryKey: ["certification-post", postId] });
    },
  });
};
