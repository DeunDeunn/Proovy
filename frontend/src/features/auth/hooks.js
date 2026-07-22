"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  checkNicknameDuplicate,
  deleteProfileImage,
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

export const useDeleteProfileImage = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteProfileImage,
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
    onSuccess: async () => {
      // 로그아웃 시점에 auth/me 요청이 이미 진행 중이었다면, 그 응답이 setQueryData(null) 이후에
      // 늦게 도착해서 로그인 상태의 me로 다시 덮어쓸 수 있다. 먼저 취소해서 그 결과를 무시하게 한다.
      await queryClient.cancelQueries({ queryKey: ["auth", "me"], exact: true });

      // 이미 마운트된 useQuery 구독자(Sidebar, NotificationRealtimeBridge 등 로그아웃해도
      // 언마운트되지 않는 컴포넌트)에 즉시 반영되려면, 그 쿼리 객체와 구독자가 아직 살아있는
      // 상태에서 setQueryData를 먼저 호출해야 한다. clear()를 먼저 하면 캐시가 지워지면서
      // setQueryData가 구독자 없는 새 쿼리 객체에 값을 넣게 되어 반영되지 않는다.
      queryClient.setQueryData(["auth", "me"], null);
      // auth/me뿐 아니라 mypage 등 이전 사용자 캐시가 남아있으면 같은 기기에서 다른 계정으로
      // 재로그인 시 잠깐 노출될 수 있어, 방금 갱신한 auth/me만 남기고 나머지를 비운다.
      queryClient.removeQueries({
        predicate: (query) => !(query.queryKey[0] === "auth" && query.queryKey[1] === "me"),
      });
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
