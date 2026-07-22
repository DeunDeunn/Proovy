"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useMe } from "@/features/auth/hooks";
import { useChatRealtimeSync } from "@/features/chat/hooks/chatHooks";
import { useNotificationRealtimeSync } from "@/features/notification/hooks/notificationHooks";

const PROFILE_SETUP_PATH = "/auth/complete-profile";
const PROFILE_GUARD_EXEMPT_PATHS = [PROFILE_SETUP_PATH, "/auth/callback", "/login"];

// 닉네임(프로필) 설정을 마치지 않은 사용자가 사이드바 링크 클릭이나 주소창 직접 이동으로
// 다른 화면에 들어가는 걸 막는다. me는 이미 앱 전역에서 구독 중이라 추가 요청은 없다.
const ProfileGuardBridge = () => {
  const { data: me } = useMe();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (me?.profileIncomplete && !PROFILE_GUARD_EXEMPT_PATHS.includes(pathname)) {
      router.replace(PROFILE_SETUP_PATH);
    }
  }, [me, pathname, router]);

  return null;
};

// QueryClientProvider 안쪽에서 렌더링되어야 useQueryClient()를 쓸 수 있어서, 화면에 그릴 게 없어도 별도 컴포넌트로 분리한다.
// 비로그인 상태에서 SSE 연결이 열리지 않도록 인증 상태(me)가 확인된 경우에만 구독을 활성화한다.
const NotificationRealtimeBridge = () => {
  const { data: me } = useMe();
  useNotificationRealtimeSync({ enabled: !!me });
  return null;
};

const ChatRealtimeBridge = () => {
  useChatRealtimeSync();
  return null;
};

const Providers = ({ children }) => {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            retry: 1, // 실패 시 1번만 재시도
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ProfileGuardBridge />
      <NotificationRealtimeBridge />
      <ChatRealtimeBridge />
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
};

export default Providers;
