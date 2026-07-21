"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";

import { isSafeRedirectPath, POST_LOGIN_REDIRECT_KEY } from "./AuthPage";

const CallbackPage = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const success = searchParams.get("success") === "true";

  useEffect(() => {
    if (success) {
      // accessToken/refreshToken은 백엔드가 이미 httpOnly 쿠키로 심어준 상태.
      // /auth/me를 다시 불러와서 로그인 상태를 화면에 반영한다.
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      const redirect = sessionStorage.getItem(POST_LOGIN_REDIRECT_KEY);
      sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
      router.replace(isSafeRedirectPath(redirect) ? redirect : "/");
    }
  }, [success, queryClient, router]);

  if (success) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-sm text-gray-500">로그인 처리 중입니다...</p>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col items-center justify-center gap-4">
      <p className="text-sm text-gray-700">로그인에 실패했습니다. 다시 시도해주세요.</p>
      <button
        type="button"
        onClick={() => router.replace("/login")}
        className="cursor-pointer rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-hover"
      >
        로그인 페이지로 돌아가기
      </button>
    </div>
  );
};

export default CallbackPage;
