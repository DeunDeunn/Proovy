"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { applyVerification, getMyPage, getVerificationStatus } from "./api";

export const useMyPage = () =>
  useQuery({
    queryKey: ["mypage"],
    queryFn: getMyPage,
  });

export const useVerificationStatus = () =>
  useQuery({
    queryKey: ["mypage", "verification-status"],
    queryFn: getVerificationStatus,
  });

export const useApplyVerification = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: applyVerification,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
    },
  });
};
