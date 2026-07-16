"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelChallenge,
  createChallenge,
  getCategories,
  getChallenge,
  getChallenges,
  updateChallenge,
} from "./api";
import { challengeKeys } from "./queryKeys";

export const useCategories = () =>
  useQuery({ queryKey: challengeKeys.categories(), queryFn: getCategories });

export const useChallenges = (params) =>
  useQuery({ queryKey: challengeKeys.list(params), queryFn: () => getChallenges(params) });

export const useChallenge = (challengeId) =>
  useQuery({
    queryKey: challengeKeys.detail(challengeId),
    queryFn: () => getChallenge(challengeId),
    enabled: !!challengeId,
  });

export const useCreateChallenge = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createChallenge,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: challengeKeys.lists() }),
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

export const useCancelChallenge = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: cancelChallenge,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: challengeKeys.lists() }),
  });
};
