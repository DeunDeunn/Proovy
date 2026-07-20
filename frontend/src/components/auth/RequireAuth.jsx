"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";

// CustomAuthenticationEntryPoint / CurrentUser.getUserId()가 비로그인 요청에 내려주는 코드.
const UNAUTHENTICATED_CODE = "C004";

/**
 * 로그인한 사용자만 접근 가능한 화면을 감싸는 가드.
 * 비로그인(401, C004) 상태면 /login으로 리다이렉트하고,
 * 그 외 네트워크/서버 오류는 로그인 상태를 오해하지 않도록 재시도 UI를 보여준다.
 */
const RequireAuth = ({ children }) => {
  const router = useRouter();
  const { data: me, isLoading, isFetching, isError, error, refetch } = useMe();

  const isUnauthenticated = isError && error?.code === UNAUTHENTICATED_CODE;

  useEffect(() => {
    if (!isLoading && isUnauthenticated) {
      router.replace("/login");
    }
  }, [isLoading, isUnauthenticated, router]);

  if (isLoading || isUnauthenticated) return <Loading />;

  if (isError) {
    return (
      <div className="flex flex-col items-center gap-3 py-12">
        <ErrorMessage error={error} />
        <Button variant="outline" onClick={() => refetch()} disabled={isFetching}>
          다시 시도
        </Button>
      </div>
    );
  }

  if (!me) return <Loading />;

  return children;
};

export default RequireAuth;
