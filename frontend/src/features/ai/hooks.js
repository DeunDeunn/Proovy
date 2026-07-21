"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { walletKeys } from "@/features/wallet/queryKeys";

import {
  getActiveAiTicket,
  getAiTicketHistory,
  getAiTicketPlans,
  purchaseAiTicket,
} from "./api";
import { aiTicketKeys } from "./queryKeys";

export const useAiTicketPlans = () =>
  useQuery({ queryKey: aiTicketKeys.plans(), queryFn: getAiTicketPlans });

export const useActiveAiTicket = () =>
  useQuery({ queryKey: aiTicketKeys.active(), queryFn: getActiveAiTicket });

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
