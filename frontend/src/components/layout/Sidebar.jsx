"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
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
} from "lucide-react";

import Image from "next/image";

import { useUnreadChatCount } from "@/features/chat/store";
import { useUnreadNotificationCount } from "@/features/notification/store";

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

const mypageMenus = [
  { name: "내 정보", href: "/mypage" },
  { name: "내 인증피드", href: "/mypage/feed" },
  { name: "우수 사용자 인증", href: "/mypage/verification" },
  { name: "AI 티켓 관리", href: "/mypage/tickets" },
  { name: "설정", href: "/mypage/settings" },
  { name: "회원탈퇴", href: "/mypage/delete" },
];

const SidebarDropdown = ({ icon: Icon, label, items, pathname }) => {
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
  const unreadNotificationCount = useUnreadNotificationCount();
  const unreadChatCount = useUnreadChatCount();

  return (
    <aside className="flex h-full w-60 shrink-0 flex-col overflow-y-auto border-r border-gray-200 bg-surface px-4 py-6">
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

      {/* 메인 메뉴 */}
      <nav className="flex flex-col gap-1">
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
              {menu.href === "/chat" && unreadChatCount > 0 && (
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

        <SidebarDropdown icon={User} label="마이페이지" items={mypageMenus} pathname={pathname} />
      </nav>

      {/* 하단 프로필 자리 */}
      <Link
        href="/login"
        className="mt-auto flex items-center gap-3 rounded-lg px-3 py-2 hover:bg-gray-50"
      >
        <div className="h-8 w-8 rounded-full bg-gray-200" />
        <span className="text-sm text-gray-700">로그인 / 회원가입</span>
      </Link>
    </aside>
  );
};

export default Sidebar;
