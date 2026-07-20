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
      // auth/me뿐 아니라 mypage 등 이전 사용자 캐시가 남아있으면
      // 같은 기기에서 다른 계정으로 재로그인 시 잠깐 노출될 수 있어 전체를 비운다.
      queryClient.clear();
    },
  });
};

export const useWithdraw = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: withdraw,
    onSuccess: () => {
      queryClient.clear();
    },
  });
};
