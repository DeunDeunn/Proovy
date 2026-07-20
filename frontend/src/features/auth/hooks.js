"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { checkNicknameDuplicate, getMe, logout, updateNickname } from "./api";

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

export const useLogout = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(["auth", "me"], undefined);
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
};
