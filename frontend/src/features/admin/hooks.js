"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  completeWithdrawal,
  getReportList,
  getVerificationList,
  getWithdrawalList,
  processReport,
  rejectReport,
  rejectWithdrawal,
  reviewVerification,
} from "./api";

export const useVerificationList = (status) =>
  useQuery({
    queryKey: ["admin", "user-verifications", { status }],
    queryFn: () => getVerificationList({ status }),
  });

export const useReviewVerification = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, status, rejectionReason }) => reviewVerification(id, { status, rejectionReason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "user-verifications"] });
    },
  });
};

export const useReportList = (targetType) =>
  useQuery({
    queryKey: ["admin", "reports", { targetType }],
    queryFn: () => getReportList({ targetType }),
  });

export const useProcessReport = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reportId) => processReport(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "reports"] });
    },
  });
};

export const useRejectReport = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reportId) => rejectReport(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "reports"] });
    },
  });
};

export const useWithdrawalList = (status) =>
  useQuery({
    queryKey: ["admin", "withdrawals", { status }],
    queryFn: () => getWithdrawalList({ status }),
  });

export const useCompleteWithdrawal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (withdrawalId) => completeWithdrawal(withdrawalId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "withdrawals"] });
    },
  });
};

export const useRejectWithdrawal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ withdrawalId, rejectReason }) => rejectWithdrawal(withdrawalId, rejectReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "withdrawals"] });
    },
  });
};
