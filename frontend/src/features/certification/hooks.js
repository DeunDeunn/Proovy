"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  approveCertificationPost,
  createCertificationPost,
  createComment,
  createReport,
  deleteCertificationPost,
  deleteComment,
  getCertificationPost,
  getComments,
  getPendingCertifications,
  rejectCertificationPost,
  runAiReview,
  toggleCommentLike,
  toggleCertificationPostLike,
  updateComment,
  updateCertificationPost,
} from "./api";

const COMMENT_PAGE_SIZE = 20;
const PENDING_CERTIFICATIONS_PAGE_SIZE = 20;

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

export const useCreateReport = () =>
  useMutation({
    mutationFn: (payload) => createReport(payload),
  });

export const useDeleteCertificationPost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (postId) => deleteCertificationPost(postId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["feed"] });
      queryClient.invalidateQueries({ queryKey: ["challenge-feed"] });
      queryClient.invalidateQueries({ queryKey: ["certification-post"] });
    },
  });
};

export const useToggleCertificationPostLike = (postId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => toggleCertificationPostLike(postId),
    onSuccess: ({ liked, likeCount }) => {
      queryClient.setQueryData(["certification-post", postId], (post) =>
        post ? { ...post, liked, likeCount } : post
      );
      queryClient.invalidateQueries({ queryKey: ["feed"] });
      queryClient.invalidateQueries({ queryKey: ["challenge-feed"] });
    },
  });
};

const updateCommentLikeState = (comment, commentId, liked, likeCount) => {
  if (comment.commentId === commentId) {
    return { ...comment, liked, likeCount };
  }

  if (!comment.replies) return comment;

  return {
    ...comment,
    replies: comment.replies.map((reply) =>
      reply.commentId === commentId ? { ...reply, liked, likeCount } : reply
    ),
  };
};

export const useToggleCommentLike = (postId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (commentId) => toggleCommentLike(commentId),
    onSuccess: ({ liked, likeCount }, commentId) => {
      queryClient.setQueryData(["certification-comments", postId], (data) =>
        data
          ? {
              ...data,
              pages: data.pages.map((page) =>
                page.map((comment) => updateCommentLikeState(comment, commentId, liked, likeCount))
              ),
            }
          : data
      );
    },
  });
};

export const useUpdateComment = (postId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ commentId, payload }) => updateComment(commentId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["certification-comments", postId] });
    },
  });
};

export const useDeleteComment = (postId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (commentId) => deleteComment(commentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["certification-comments", postId] });
      queryClient.invalidateQueries({ queryKey: ["certification-post", postId] });
    },
  });
};

export const usePendingCertifications = (challengeId, { enabled = true } = {}) =>
  useInfiniteQuery({
    queryKey: ["pending-certifications", challengeId],
    queryFn: ({ pageParam }) =>
      getPendingCertifications(challengeId, {
        cursor: pageParam,
        size: PENDING_CERTIFICATIONS_PAGE_SIZE,
      }),
    initialPageParam: null,
    enabled: !!challengeId && enabled,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < PENDING_CERTIFICATIONS_PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.postId;
    },
  });

const invalidatePendingCertifications = (queryClient, challengeId) =>
  Promise.all([
    queryClient.invalidateQueries({ queryKey: ["pending-certifications", challengeId] }),
    queryClient.invalidateQueries({
      queryKey: ["challenges", "detail", challengeId, "participants-manage"],
    }),
  ]);

export const useApproveCertificationPost = (challengeId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (postId) => approveCertificationPost(postId),
    onSuccess: () => invalidatePendingCertifications(queryClient, challengeId),
  });
};

export const useRejectCertificationPost = (challengeId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ postId, reason }) => rejectCertificationPost(postId, reason),
    onSuccess: () => invalidatePendingCertifications(queryClient, challengeId),
  });
};

export const useAiReviewCertificationPost = (challengeId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (postId) => runAiReview(postId),
    onSuccess: () => invalidatePendingCertifications(queryClient, challengeId),
  });
};
