"use client";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import LoginRequiredModal from "@/components/ui/LoginRequiredModal";
import { useMe } from "@/features/auth/hooks";

// CustomAuthenticationEntryPoint / CurrentUser.getUserId()가 비로그인 요청에 내려주는 코드.
const UNAUTHENTICATED_CODE = "C004";

/**
 * 로그인한 사용자만 접근 가능한 화면을 감싸는 가드.
 * 비로그인(401, C004) 상태면 LoginRequiredModal을 보여주고(채팅 도메인과 동일 컨벤션),
 * 그 외 네트워크/서버 오류는 로그인 상태를 오해하지 않도록 재시도 UI를 보여준다.
 */
const RequireAuth = ({ children }) => {
  const { data: me, isLoading, isFetching, isError, error, refetch } = useMe();

  const isUnauthenticated = isError && error?.code === UNAUTHENTICATED_CODE;

  if (isLoading) return <Loading />;

  if (isUnauthenticated) return <LoginRequiredModal />;

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
