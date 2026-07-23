"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { Bell, BellOff, Check, MoreVertical } from "lucide-react";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";
import LoginRequiredModal from "@/components/ui/LoginRequiredModal";
import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";
import NotificationCard from "@/features/notification/components/NotificationCard";
import {
  FILTER_GROUPS,
  FILTER_GROUP_TO_CATEGORY,
  formatDateLabel,
} from "@/features/notification/notificationMeta";
import {
  useDeleteAllNotifications,
  useDeleteNotification,
  useMarkAllAsRead,
  useMarkAsRead,
  useNotifications,
  useUnreadCount,
} from "@/features/notification/hooks/notificationHooks";

const NotificationPage = () => {
  const { data: me, isLoading: isMeLoading, isError: isMeError, error: meError } = useMe();
  // 로그아웃 시 setQueryData(["auth","me"], null)로 me가 null이 되는데, 이건 에러가 아니라
  // 성공 상태라서 그 케이스도 명시적으로 잡아줘야 페이지를 보고 있는 도중 로그아웃해도 바로 막힌다.
  const isUnauthorized = (isMeError && meError?.status === 401) || (!isMeLoading && me === null);

  const [activeGroup, setActiveGroup] = useState("전체");
  const [menuOpen, setMenuOpen] = useState(false);
  const sentinelRef = useRef(null);

  const category = FILTER_GROUP_TO_CATEGORY[activeGroup];
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useNotifications(
    category,
    { enabled: !!me }
  );
  const { data: unreadCountData } = useUnreadCount({ enabled: !!me });
  const markAsReadMutation = useMarkAsRead();
  const markAllAsReadMutation = useMarkAllAsRead();
  const deleteMutation = useDeleteNotification();
  const deleteAllMutation = useDeleteAllNotifications();

  // 서버가 이미 category로 필터링해서 주므로, 여기 담긴 목록은 항상 현재 필터에 맞는 알림만 있다.
  const visibleItems = useMemo(() => data?.pages.flatMap((page) => page.content) ?? [], [data]);
  const unreadCount = unreadCountData?.unreadCount ?? 0;

  const handleRead = (notification) => {
    if (notification.readAt != null) return;
    markAsReadMutation.mutate(notification.id);
  };

  const handleClearAll = () => {
    deleteAllMutation.mutate();
    setMenuOpen(false);
  };

  useEffect(() => {
    if (isLoading || !hasNextPage) return;

    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || isFetchingNextPage) return;
        fetchNextPage();
      },
      { rootMargin: "200px" }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [isLoading, hasNextPage, isFetchingNextPage, fetchNextPage]);

  if (isMeLoading) return null;
  if (isUnauthorized)
    return <LoginRequiredModal description="알림은 로그인 후 이용할 수 있어요." />;
  if (isMeError) {
    return (
      <div className="mx-auto flex max-w-3xl items-center justify-center px-4 py-16">
        <ErrorMessage error={meError} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="max-w-3xl">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-2xl font-bold text-gray-900">
              <Bell size={24} />
              알림
            </h1>
            <p className="mt-1 text-sm text-gray-500">
              안 읽은 알림 <span className="font-semibold text-primary">{unreadCount}개</span>
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              className="flex items-center gap-2"
              onClick={() => markAllAsReadMutation.mutate()}
              disabled={unreadCount === 0}
            >
              <Check size={16} />
              모두 읽음
            </Button>

            <div className="relative">
              <button
                type="button"
                onClick={() => setMenuOpen((prev) => !prev)}
                className="rounded-xl p-2 text-gray-500 transition-colors hover:bg-gray-100"
                aria-label="더보기"
              >
                <MoreVertical size={18} />
              </button>

              {menuOpen && (
                <>
                  <div className="fixed inset-0 z-10" onClick={() => setMenuOpen(false)} />
                  <div className="animate-[dropdown-in_120ms_ease-out] absolute right-0 top-full z-20 mt-2 w-36 origin-top-right rounded-2xl border border-gray-100 bg-surface p-1.5 shadow-xl shadow-black/5">
                    <button
                      type="button"
                      onClick={handleClearAll}
                      className="w-full rounded-xl px-3 py-2 text-left text-sm text-danger transition-colors hover:bg-red-50"
                    >
                      전체 삭제
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="mt-6 flex gap-2 border-b border-gray-100 pb-5">
          {FILTER_GROUPS.map((group) => (
            <button
              key={group}
              type="button"
              onClick={() => setActiveGroup(group)}
              className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
                activeGroup === group
                  ? "bg-primary text-white"
                  : "border border-gray-200 bg-white text-gray-600 hover:bg-gray-50"
              }`}
            >
              {group}
            </button>
          ))}
        </div>

        {isLoading ? (
          <Loading label="알림을 불러오는 중..." />
        ) : (
          <>
            <div className="mt-5 flex flex-col gap-2">
              {visibleItems.length === 0 && activeGroup === "전체" && (
                <div className="flex flex-col items-center gap-4 rounded-2xl border border-gray-100 bg-surface py-16 text-center shadow-xl shadow-black/5">
                  <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-400">
                    <BellOff size={26} />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-700">아직 알림이 없어요</p>
                    <p className="mt-1 text-sm text-gray-400">
                      관심있는 챌린지에 참여하고 소식을 받아보세요
                    </p>
                  </div>
                  <Link
                    href="/challenges"
                    className="cursor-pointer rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-hover"
                  >
                    챌린지 둘러보기
                  </Link>
                </div>
              )}
              {visibleItems.length === 0 && activeGroup !== "전체" && (
                <p className="py-12 text-center text-sm text-gray-400">해당하는 알림이 없습니다.</p>
              )}
              {visibleItems.map((notification, index) => {
                const dateLabel = formatDateLabel(new Date(notification.createdAt));
                const prevDateLabel =
                  index > 0 ? formatDateLabel(new Date(visibleItems[index - 1].createdAt)) : null;

                return (
                  <div key={notification.id}>
                    {dateLabel !== prevDateLabel && (
                      <p
                        className={`mb-1 text-xs font-semibold text-gray-400 ${index === 0 ? "" : "mt-4"}`}
                      >
                        {dateLabel}
                      </p>
                    )}
                    <NotificationCard
                      notification={notification}
                      onRead={handleRead}
                      onDelete={(id) => deleteMutation.mutate(id)}
                    />
                  </div>
                );
              })}
            </div>

            <div ref={sentinelRef} />
            {isFetchingNextPage && <Loading label="알림을 더 불러오는 중..." />}
            {!hasNextPage && visibleItems.length > 0 && (
              <p className="py-6 text-center text-sm text-gray-400">모든 알림을 확인했어요.</p>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default NotificationPage;
