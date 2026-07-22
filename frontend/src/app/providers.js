"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { useState } from "react";

import { useMe } from "@/features/auth/hooks";
import { useChatRealtimeSync } from "@/features/chat/hooks/chatHooks";
import { useNotificationRealtimeSync } from "@/features/notification/hooks/notificationHooks";

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
      <NotificationRealtimeBridge />
      <ChatRealtimeBridge />
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
};

export default Providers;
