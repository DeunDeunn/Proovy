"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  checkNicknameDuplicate,
  getMe,
  logout,
  updateNickname,
  updateProfileImage,
  withdraw,
} from "./api";

export const useMe = () =>
  useQuery({
    queryKey: ["auth", "me"],
    queryFn: getMe,
    retry: false,
  });

export const useCheckNicknameDuplicate = () =>
  useMutation({
    mutationFn: (nickname) => checkNicknameDuplicate(nickname),
  });

export const useUpdateNickname = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (nickname) => updateNickname(nickname),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
};

export const useUpdateProfileImage = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (file) => updateProfileImage(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
};

export const useLogout = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ["auth", "me"] });
    },
  });
};

export const useWithdraw = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: withdraw,
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ["auth", "me"] });
    },
  });
};
