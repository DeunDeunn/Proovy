"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelChallenge,
  createChallenge,
  getCategories,
  getChallenge,
  getChallengeParticipants,
  getChallenges,
  joinChallenge,
  leaveChallenge,
  updateChallenge,
  updateChallengeThumbnail,
} from "./api";
import { challengeKeys } from "./queryKeys";
import { walletKeys } from "@/features/wallet/queryKeys";

export const useCategories = () =>
  useQuery({ queryKey: challengeKeys.categories(), queryFn: getCategories });

export const useChallenges = (params) =>
  useQuery({ queryKey: challengeKeys.list(params), queryFn: () => getChallenges(params) });

export const useInfiniteChallenges = (params) =>
  useInfiniteQuery({
    queryKey: [...challengeKeys.list(params), "infinite"],
    queryFn: ({ pageParam }) => getChallenges({ ...params, page: pageParam }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined,
  });

export const useChallenge = (challengeId) =>
  useQuery({
    queryKey: challengeKeys.detail(challengeId),
    queryFn: () => getChallenge(challengeId),
    enabled: !!challengeId,
  });

export const useChallengeParticipants = (challengeId) =>
  useQuery({
    queryKey: challengeKeys.participants(challengeId),
    queryFn: () => getChallengeParticipants(challengeId),
    enabled: !!challengeId,
  });

export const useJoinChallenge = (challengeId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => joinChallenge(challengeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: challengeKeys.detail(challengeId) });
      queryClient.invalidateQueries({ queryKey: challengeKeys.participants(challengeId) });
      queryClient.invalidateQueries({ queryKey: challengeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: walletKeys.me() });
    },
  });
};

export const useLeaveChallenge = (challengeId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => leaveChallenge(challengeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: challengeKeys.detail(challengeId) });
      queryClient.invalidateQueries({ queryKey: challengeKeys.participants(challengeId) });
      queryClient.invalidateQueries({ queryKey: challengeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: walletKeys.me() });
    },
  });
};

export const useCreateChallenge = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createChallenge,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: challengeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: walletKeys.me() });
    },
  });
};

export const useUpdateChallenge = (challengeId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload) => updateChallenge(challengeId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: challengeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: challengeKeys.detail(challengeId) });
    },
  });
};

export const useUpdateChallengeThumbnail = (challengeId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file) => updateChallengeThumbnail(challengeId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: challengeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: challengeKeys.detail(challengeId) });
    },
  });
};

export const useCancelChallenge = (challengeId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: cancelChallenge,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: challengeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: challengeKeys.detail(challengeId) });
    },
  });
};
