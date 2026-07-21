"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, useEffect } from "react";

import {
  Home,
  Trophy,
  MessageSquare,
  MessageCircle,
  Bell,
  Flag,
  Wallet,
  User,
  ChevronDown,
  ChevronUp,
  LogIn,
  LogOut,
  Plus,
} from "lucide-react";

import Image from "next/image";

import { useUnreadChatCount } from "@/features/chat/store";
import { useUnreadCount } from "@/features/notification/hooks/notificationHooks";
import { useLogout, useMe } from "@/features/auth/hooks";
import { DEFAULT_PROFILE_IMAGE_URL } from "@/lib/constants";

const menus = [
  { name: "홈", href: "/", icon: Home },
  { name: "챌린지", href: "/challenges", icon: Trophy },
  { name: "인증 피드", href: "/feeds", icon: MessageSquare },
  { name: "채팅", href: "/chat", icon: MessageCircle },
  { name: "알림", href: "/notifications", icon: Bell },
  { name: "내 챌린지", href: "/my-challenges", icon: Flag },
];

const walletMenus = [
  { name: "내 지갑", href: "/wallet" },
  { name: "충전하기", href: "/wallet/charge" },
  { name: "출금하기", href: "/wallet/withdraw" },
  { name: "정산 내역", href: "/wallet/settlements" },
];

const baseMypageMenus = [
  { name: "내 정보", href: "/mypage" },
  { name: "내 인증피드", href: "/mypage/feed" },
  { name: "우수 사용자 인증", href: "/mypage/verification" },
  { name: "AI 티켓 관리", href: "/mypage/tickets" },
  { name: "설정", href: "/mypage/settings" },
];
const withdrawMenu = { name: "회원탈퇴", href: "/mypage/withdraw" };

const SidebarDropdown = ({ icon: Icon, label, items, pathname, onItemAction }) => {
  const isActive = items.some((m) => m.href === pathname);
  const [manualOpen, setManualOpen] = useState(false);
  const [prevPathname, setPrevPathname] = useState(pathname);

  // 렌더링 도중 pathname이 바뀐 걸 감지하면, 그 자리에서 바로 리셋
  if (pathname !== prevPathname) {
    setPrevPathname(pathname);
    setManualOpen(false);
  }

  const open = isActive || manualOpen;

  return (
    <div>
      <button
        onClick={() => setManualOpen((prev) => !prev)}
        className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-gray-600 hover:bg-gray-50"
      >
        <Icon size={18} />
        {label}
        {open ? (
          <ChevronUp size={16} className="ml-auto" />
        ) : (
          <ChevronDown size={16} className="ml-auto" />
        )}
      </button>
      {open &&
        items.map((menu) => {
          if (menu.action) {
            return (
              <button
                key={menu.name}
                type="button"
                onClick={() => onItemAction(menu.action)}
                className="block w-full rounded-lg py-2 pl-11 pr-3 text-left text-sm text-gray-600 hover:bg-gray-50"
              >
                {menu.name}
              </button>
            );
          }

          const active = pathname === menu.href;
          return (
            <Link
              key={menu.href}
              href={menu.href}
              className={`block rounded-lg py-2 pl-11 pr-3 text-sm ${
                active
                  ? "bg-primary-light font-semibold text-primary"
                  : "text-gray-600 hover:bg-gray-50"
              }`}
            >
              {menu.name}
            </Link>
          );
        })}
    </div>
  );
};

const Sidebar = () => {
  const pathname = usePathname();
  const router = useRouter();
  const { data: unreadCountData } = useUnreadCount();
  const unreadNotificationCount = unreadCountData?.unreadCount ?? 0;
  const unreadChatCount = useUnreadChatCount();
  const { data: me, isLoading: isMeLoading } = useMe();
  const logoutMutation = useLogout();

  const mypageMenuItems = [...baseMypageMenus, withdrawMenu];

  const handleLogout = () => {
    logoutMutation.mutate(undefined, {
      onSuccess: () => router.replace("/"),
    });
  };

  return (
    <aside className="flex h-full w-60 shrink-0 flex-col border-r border-gray-200 bg-surface px-4 py-6">
      {/* 로고 */}
      <Link href="/" className="mb-8 px-3">
        <Image
          src="/logo.png"
          alt="Proovy"
          width={827}
          height={281}
          className="h-6 w-auto"
          priority
        />
      </Link>

      {/* 메인 메뉴: 드롭다운이 펼쳐져도 아래 버튼/프로필 위치가 안 밀리게 이 영역만 따로 스크롤 */}
      <nav className="flex flex-1 flex-col gap-1 overflow-y-auto">
        {menus.map((menu) => {
          const active = pathname === menu.href;
          return (
            <Link
              key={menu.href}
              href={menu.href}
              className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm ${
                active
                  ? "bg-primary-light font-semibold text-primary"
                  : "text-gray-600 hover:bg-gray-50"
              }`}
            >
              <menu.icon size={18} />
              {menu.name}
              {menu.href === "/notifications" && unreadNotificationCount > 0 && (
                <span className="ml-auto rounded-full bg-primary px-1.5 py-0.5 text-xs font-semibold text-white">
                  {unreadNotificationCount}
                </span>
              )}
              {menu.href === "/chat" && me && unreadChatCount > 0 && (
                <span className="ml-auto rounded-full bg-primary px-1.5 py-0.5 text-xs font-semibold text-white">
                  {unreadChatCount}
                </span>
              )}
            </Link>
          );
        })}

        <div className="my-3 border-t border-gray-200" />

        <SidebarDropdown icon={Wallet} label="지갑" items={walletMenus} pathname={pathname} />

        <div className="my-3 border-t border-gray-200" />

        <SidebarDropdown
          icon={User}
          label="마이페이지"
          items={mypageMenuItems}
          pathname={pathname}
        />
      </nav>

      {/* 로그인 안 한 사용자는 어차피 개설할 수 없으니 로그인했을 때만 노출 */}
      {me && (
        <Link
          href="/challenges/new"
          className="mt-4 flex items-center justify-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-white hover:bg-primary-hover"
        >
          <Plus size={16} />
          챌린지 개설하기
        </Link>
      )}

      {/* 하단 프로필 자리: 위 메뉴/버튼과 충분한 여백만으로 구분 */}
      <div className="mt-6">
        {isMeLoading ? (
          <div className="h-11 animate-pulse rounded-lg bg-gray-100" />
        ) : me ? (
          <div className="flex items-center gap-1 rounded-lg px-3 py-2">
            <Link
              href="/mypage"
              className="flex min-w-0 flex-1 items-center gap-3 rounded-lg hover:bg-gray-50"
            >
              <img
                src={me.profileImageUrl || DEFAULT_PROFILE_IMAGE_URL}
                alt={`${me.nickname} 프로필 이미지`}
                className="h-8 w-8 rounded-full border border-gray-200 object-cover"
              />
              <span className="truncate text-sm font-medium text-gray-700">{me.nickname}</span>
            </Link>
            <button
              type="button"
              onClick={handleLogout}
              title="로그아웃"
              className="cursor-pointer rounded-lg p-2 text-gray-400 hover:bg-gray-50 hover:text-gray-700"
            >
              <LogOut size={16} />
            </button>
          </div>
        ) : (
          <Link
            href="/login"
            className="flex items-center justify-center gap-2 rounded-lg bg-primary px-3 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover"
          >
            <LogIn size={16} />
            로그인 / 회원가입
          </Link>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
