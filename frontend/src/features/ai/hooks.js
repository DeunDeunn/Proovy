"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { walletKeys } from "@/features/wallet/queryKeys";
import { challengeKeys } from "@/features/challenge/queryKeys";

import {
  getActiveAiTicket,
  getAiTicketHistory,
  getAiTicketPlans,
  deactivateAiReview,
  getAiReviewResults,
  getAiReviewRule,
  purchaseAiTicket,
  requestAiReview,
  updateAiReviewMode,
  upsertAiReviewRule,
} from "./api";
import { aiReviewKeys, aiTicketKeys } from "./queryKeys";

export const useAiTicketPlans = () =>
  useQuery({ queryKey: aiTicketKeys.plans(), queryFn: getAiTicketPlans });

export const useActiveAiTicket = () =>
  useQuery({ queryKey: aiTicketKeys.active(), queryFn: getActiveAiTicket });

export const useHasActiveAiTicket = () =>
  useQuery({
    queryKey: aiTicketKeys.active(),
    queryFn: getActiveAiTicket,
    select: (ticket) => ticket?.hasActiveTicket === true,
  });

export const useAiTicketHistory = (params) =>
  useQuery({
    queryKey: aiTicketKeys.history(params),
    queryFn: () => getAiTicketHistory(params),
  });

export const usePurchaseAiTicket = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: purchaseAiTicket,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: aiTicketKeys.all });
      queryClient.invalidateQueries({ queryKey: walletKeys.all });
    },
  });
};

export const useAiReviewRule = (challengeId, { enabled = true } = {}) =>
  useQuery({
    queryKey: aiReviewKeys.rule(challengeId),
    queryFn: () => getAiReviewRule(challengeId),
    enabled: Boolean(challengeId) && enabled,
  });

export const useAiReviewResults = (challengeId, params) =>
  useQuery({
    queryKey: aiReviewKeys.resultList(challengeId, params),
    queryFn: () => getAiReviewResults(challengeId, params),
    enabled: Boolean(challengeId),
  });

const syncAiReviewEnabled = (queryClient, challengeId, enabled) => {
  queryClient.setQueryData(challengeKeys.detail(challengeId), (challenge) =>
    challenge ? { ...challenge, aiReviewEnabled: enabled } : challenge
  );
};

const invalidateAiReviewSettings = (queryClient, challengeId) =>
  Promise.all([
    queryClient.invalidateQueries({ queryKey: aiReviewKeys.rule(challengeId) }),
    queryClient.invalidateQueries({ queryKey: challengeKeys.detail(challengeId) }),
  ]);

export const useUpsertAiReviewRule = (challengeId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload) => upsertAiReviewRule(challengeId, payload),
    onSuccess: (savedRule) => {
      queryClient.setQueryData(aiReviewKeys.rule(challengeId), savedRule);
      syncAiReviewEnabled(queryClient, challengeId, true);
      return invalidateAiReviewSettings(queryClient, challengeId);
    },
  });
};

export const useUpdateAiReviewMode = (challengeId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reviewMode) => updateAiReviewMode(challengeId, reviewMode),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: aiReviewKeys.rule(challengeId) }),
  });
};

export const useDeactivateAiReview = (challengeId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => deactivateAiReview(challengeId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: aiReviewKeys.rule(challengeId), exact: true });
      syncAiReviewEnabled(queryClient, challengeId, false);
      return invalidateAiReviewSettings(queryClient, challengeId);
    },
  });
};

export const useRequestAiReview = (challengeId) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: requestAiReview,
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: aiReviewKeys.results() }),
        queryClient.invalidateQueries({ queryKey: ["pending-certifications", challengeId] }),
        queryClient.invalidateQueries({ queryKey: ["challenge-feed", challengeId] }),
        queryClient.invalidateQueries({ queryKey: aiTicketKeys.active() }),
      ]),
  });
};
