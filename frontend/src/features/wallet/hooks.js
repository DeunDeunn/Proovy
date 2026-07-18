"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  applyWithdrawal,
  getMyTransactions,
  getMyWallet,
  getMyWithdrawals,
  getSettlementResult,
  getWithdrawableAmount,
  requestCharge,
} from "./api";
import { settlementKeys, walletKeys, withdrawalKeys } from "./queryKeys";

export const useWallet = () => useQuery({ queryKey: walletKeys.me(), queryFn: getMyWallet });

export const useWithdrawableAmount = () =>
  useQuery({ queryKey: walletKeys.withdrawableAmount(), queryFn: getWithdrawableAmount });

export const useTransactions = (params) =>
  useQuery({
    queryKey: walletKeys.transactions(params),
    queryFn: () => getMyTransactions(params),
  });

export const useMyWithdrawals = (params) =>
  useQuery({
    queryKey: withdrawalKeys.me(params),
    queryFn: () => getMyWithdrawals(params),
  });

export const useSettlementResult = (challengeId) =>
  useQuery({
    queryKey: settlementKeys.detail(challengeId),
    queryFn: () => getSettlementResult(challengeId),
    enabled: !!challengeId,
  });

export const useRequestCharge = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: requestCharge,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: walletKeys.me() }),
  });
};

export const useApplyWithdrawal = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: applyWithdrawal,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: walletKeys.all });
      queryClient.invalidateQueries({ queryKey: withdrawalKeys.all });
    },
  });
};
