"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";

/**
 * 로그인한 사용자만 접근 가능한 화면을 감싸는 가드.
 * 비로그인 상태면 /login으로 리다이렉트한다.
 */
const RequireAuth = ({ children }) => {
  const router = useRouter();
  const { data: me, isLoading, isError } = useMe();

  useEffect(() => {
    if (!isLoading && (isError || !me)) {
      router.replace("/login");
    }
  }, [isLoading, isError, me, router]);

  if (isLoading || !me) return <Loading />;

  return children;
};

export default RequireAuth;
