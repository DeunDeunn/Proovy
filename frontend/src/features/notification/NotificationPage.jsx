"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { Bell, BellOff, Check, MoreVertical } from "lucide-react";

import Button from "@/components/ui/Button";
import Loading from "@/components/ui/Loading";
import NotificationCard from "@/features/notification/NotificationCard";
import { FILTER_GROUPS, formatDateLabel } from "@/features/notification/mockData";
import { useNotificationStore } from "@/features/notification/store";

const PAGE_SIZE = 6;
const INITIAL_LOAD_DELAY = 500;

const NotificationPage = () => {
  const notifications = useNotificationStore((state) => state.notifications);
  const markRead = useNotificationStore((state) => state.markRead);
  const markAllRead = useNotificationStore((state) => state.markAllRead);
  const remove = useNotificationStore((state) => state.remove);
  const clearAll = useNotificationStore((state) => state.clearAll);

  const [activeGroup, setActiveGroup] = useState("전체");
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [loadingMore, setLoadingMore] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [menuOpen, setMenuOpen] = useState(false);
  const sentinelRef = useRef(null);

  useEffect(() => {
    const timer = setTimeout(() => setInitialLoading(false), INITIAL_LOAD_DELAY);
    return () => clearTimeout(timer);
  }, []);

  const filtered = useMemo(
    () =>
      activeGroup === "전체"
        ? notifications
        : notifications.filter((n) => n.group === activeGroup),
    [notifications, activeGroup],
  );

  const visibleItems = filtered.slice(0, visibleCount);
  const hasMore = visibleCount < filtered.length;
  const unreadCount = notifications.filter((n) => !n.read).length;

  const handleFilterChange = (group) => {
    setActiveGroup(group);
    setVisibleCount(PAGE_SIZE);
  };

  const handleClearAll = () => {
    clearAll();
    setMenuOpen(false);
  };

  useEffect(() => {
    if (initialLoading || !hasMore) return;

    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || loadingMore) return;

        setLoadingMore(true);
        setTimeout(() => {
          setVisibleCount((prev) => prev + PAGE_SIZE);
          setLoadingMore(false);
        }, 500);
      },
      { rootMargin: "200px" },
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [initialLoading, hasMore, loadingMore, activeGroup]);

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
            onClick={markAllRead}
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
            onClick={() => handleFilterChange(group)}
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

      {initialLoading ? (
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
                <Link href="/challenges">
                  <Button>챌린지 둘러보기</Button>
                </Link>
              </div>
            )}
            {visibleItems.length === 0 && notifications.length > 0 && (
              <p className="py-12 text-center text-sm text-gray-400">해당하는 알림이 없습니다.</p>
            )}
            {visibleItems.map((notification, index) => {
              const dateLabel = formatDateLabel(notification.createdAt);
              const prevDateLabel =
                index > 0 ? formatDateLabel(visibleItems[index - 1].createdAt) : null;

              return (
                <div key={notification.id}>
                  {dateLabel !== prevDateLabel && (
                    <p className={`mb-1 text-xs font-semibold text-gray-400 ${index === 0 ? "" : "mt-4"}`}>
                      {dateLabel}
                    </p>
                  )}
                  <NotificationCard notification={notification} onRead={markRead} onDelete={remove} />
                </div>
              );
            })}
          </div>

          <div ref={sentinelRef} />
          {loadingMore && <Loading label="알림을 더 불러오는 중..." />}
          {!hasMore && visibleItems.length > 0 && (
            <p className="py-6 text-center text-sm text-gray-400">모든 알림을 확인했어요.</p>
          )}
        </>
      )}
    </div>
  );
};

export default NotificationPage;
