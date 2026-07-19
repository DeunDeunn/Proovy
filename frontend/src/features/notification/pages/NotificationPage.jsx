"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { Bell, BellOff, Check, MoreVertical } from "lucide-react";

import Button from "@/components/ui/Button";
import Loading from "@/components/ui/Loading";
import NotificationCard from "@/features/notification/components/NotificationCard";
import { FILTER_GROUPS, NOTIFICATION_TYPE_META, formatDateLabel } from "@/features/notification/notificationMeta";
import {
  useDeleteAllNotifications,
  useDeleteNotification,
  useMarkAllAsRead,
  useMarkAsRead,
  useNotifications,
  useUnreadCount,
} from "@/features/notification/hooks/notificationHooks";

const NotificationPage = () => {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useNotifications();
  const { data: unreadCountData } = useUnreadCount();
  const markAsReadMutation = useMarkAsRead();
  const markAllAsReadMutation = useMarkAllAsRead();
  const deleteMutation = useDeleteNotification();
  const deleteAllMutation = useDeleteAllNotifications();

  const [activeGroup, setActiveGroup] = useState("전체");
  const [menuOpen, setMenuOpen] = useState(false);
  const sentinelRef = useRef(null);

  const notifications = useMemo(() => data?.pages.flatMap((page) => page.content) ?? [], [data]);
  const unreadCount = unreadCountData?.unreadCount ?? 0;

  const visibleItems = useMemo(
    () =>
      activeGroup === "전체"
        ? notifications
        : notifications.filter((n) => NOTIFICATION_TYPE_META[n.type]?.group === activeGroup),
    [notifications, activeGroup],
  );

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
      { rootMargin: "200px" },
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [isLoading, hasNextPage, isFetchingNextPage, fetchNextPage]);

  return (
    <div className="mx-auto max-w-3xl">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-xl font-bold text-gray-900">
            <Bell size={22} />
            알림
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            안 읽은 알림 <span className="font-semibold text-primary">{unreadCount}개</span>
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            className="flex items-center gap-1"
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
              className="rounded-lg p-2 text-gray-500 hover:bg-gray-100"
              aria-label="더보기"
            >
              <MoreVertical size={18} />
            </button>

            {menuOpen && (
              <>
                <div className="fixed inset-0 z-10" onClick={() => setMenuOpen(false)} />
                <div className="absolute right-0 top-full z-20 mt-1 w-32 rounded-lg border border-gray-200 bg-surface py-1 shadow-lg">
                  <button
                    type="button"
                    onClick={handleClearAll}
                    className="w-full px-3 py-2 text-left text-sm text-danger hover:bg-red-50"
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
                : "border border-gray-300 text-gray-600 hover:bg-gray-50"
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
            {visibleItems.length === 0 && notifications.length === 0 && (
              <div className="flex flex-col items-center gap-4 py-16 text-center">
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
            {visibleItems.length === 0 && notifications.length > 0 && (
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
  );
};

export default NotificationPage;
