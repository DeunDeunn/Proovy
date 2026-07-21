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
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
    },
  });
};

export const useUpdateProfileImage = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (file) => updateProfileImage(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      queryClient.invalidateQueries({ queryKey: ["mypage"] });
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
      // clear()는 캐시만 비울 뿐 이미 마운트된 useQuery 구독자(Sidebar, NotificationRealtimeBridge 등
      // 로그아웃해도 언마운트되지 않는 컴포넌트)에는 변경을 알리지 않는다. setQueryData로 직접 값을
      // 밀어넣어야 그 구독자들이 즉시 리렌더링되어 로그아웃 상태(me=null)를 반영한다.
      queryClient.setQueryData(["auth", "me"], null);
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
