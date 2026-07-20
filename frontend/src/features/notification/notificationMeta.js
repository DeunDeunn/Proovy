import { Bell, FilePlus, FileCheck, FileX, Coins, HandCoins, BadgeCheck, BadgeX } from "lucide-react";

export const FILTER_GROUPS = ["전체", "인증", "정산", "기타"];

// 프론트 필터 탭 → 백엔드 NotificationCategory(com.deundeun.notification.domain.NotificationCategory) 파라미터값
// "전체"는 매핑값이 없고, undefined로 넘겨서 서버가 필터 없이 전체 조회하도록 한다.
export const FILTER_GROUP_TO_CATEGORY = {
  인증: "VERIFICATION",
  정산: "SETTLEMENT",
  기타: "ETC",
};

// 백엔드 NotificationType(com.deundeun.notification.domain.NotificationType)과 1:1 매칭
export const NOTIFICATION_TYPE_META = {
  VERIFICATION_SUBMITTED: {
    group: "인증",
    tagLabel: "인증 제출",
    badgeVariant: "primary",
    icon: FilePlus,
    iconClass: "bg-primary-light text-primary",
  },
  VERIFICATION_APPROVED: {
    group: "인증",
    tagLabel: "인증 승인",
    badgeVariant: "success",
    icon: FileCheck,
    iconClass: "bg-green-50 text-success",
  },
  VERIFICATION_REJECTED: {
    group: "인증",
    tagLabel: "인증 거절",
    badgeVariant: "danger",
    icon: FileX,
    iconClass: "bg-red-50 text-danger",
  },
  SETTLEMENT_COMPLETED: {
    group: "정산",
    tagLabel: "정산 완료",
    badgeVariant: "success",
    icon: Coins,
    iconClass: "bg-green-50 text-success",
  },
  HOST_REVENUE_PAID: {
    group: "정산",
    tagLabel: "방장 수수료 지급",
    badgeVariant: "success",
    icon: HandCoins,
    iconClass: "bg-green-50 text-success",
  },
  BADGE_APPROVED: {
    group: "기타",
    tagLabel: "뱃지 승인",
    badgeVariant: "success",
    icon: BadgeCheck,
    iconClass: "bg-green-50 text-success",
  },
  BADGE_REJECTED: {
    group: "기타",
    tagLabel: "뱃지 거절",
    badgeVariant: "danger",
    icon: BadgeX,
    iconClass: "bg-red-50 text-danger",
  },
};

// 백엔드에 새 타입이 추가되거나 매핑이 잠시 어긋나도 알림 목록 전체가 죽지 않도록 하는 fallback.
const DEFAULT_NOTIFICATION_META = {
  group: "기타",
  tagLabel: "알림",
  badgeVariant: "gray",
  icon: Bell,
  iconClass: "bg-gray-100 text-gray-500",
};

export const getNotificationTypeMeta = (type) => NOTIFICATION_TYPE_META[type] ?? DEFAULT_NOTIFICATION_META;

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

export const formatRelativeTime = (date) => {
  const diff = Date.now() - date.getTime();

  if (diff < HOUR) return `${Math.max(1, Math.round(diff / MINUTE))}분 전`;
  if (diff < DAY) return `${Math.round(diff / HOUR)}시간 전`;
  return `${Math.round(diff / DAY)}일 전`;
};

const isSameDay = (a, b) =>
  a.getFullYear() === b.getFullYear() &&
  a.getMonth() === b.getMonth() &&
  a.getDate() === b.getDate();

export const formatDateLabel = (date) => {
  const now = new Date();
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);

  if (isSameDay(date, now)) return "오늘";
  if (isSameDay(date, yesterday)) return "어제";
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
};
