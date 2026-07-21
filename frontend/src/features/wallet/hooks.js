"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  applyWithdrawal,
  confirmNaverPayCharge,
  getMyTransactions,
  getMyWallet,
  getMyWithdrawals,
  getSettlementHistory,
  getSettlementResult,
  getWithdrawableAmount,
  requestCharge,
} from "./api";
import { settlementKeys, walletKeys, withdrawalKeys } from "./queryKeys";

export const useWallet = ({ enabled = true } = {}) =>
  useQuery({ queryKey: walletKeys.me(), queryFn: getMyWallet, enabled });

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

export const useSettlementHistory = (params) =>
  useQuery({
    queryKey: settlementKeys.history(params),
    queryFn: () => getSettlementHistory(params),
  });

export const useRequestCharge = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: requestCharge,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: walletKeys.me() }),
  });
};

export const useConfirmNaverPayCharge = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: confirmNaverPayCharge,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: walletKeys.all }),
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
