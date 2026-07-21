"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  getReportList,
  getVerificationList,
  processReport,
  rejectReport,
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
