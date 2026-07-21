export const formatCurrency = (amount) => `₩ ${Number(amount ?? 0).toLocaleString("ko-KR")}`;

export const TRANSACTION_TYPE_LABELS = {
  CHARGE: "충전",
  CHALLENGE_HOLD: "참가비 홀딩",
  CHALLENGE_PRINCIPAL_REFUND: "참가비 반환",
  CHALLENGE_PRINCIPAL_SUCCESS: "참가비 반환",
  CHALLENGE_PRINCIPAL_FAIL: "참가비 반영",
  CHALLENGE_PROFIT_DISTRIBUTION: "정산 수익",
  HOST_FEE: "호스트 수수료",
  WITHDRAWAL: "출금",
  WITHDRAWAL_REFUND: "출금 반환",
  AI_TICKET_PURCHASE: "AI 티켓 구매",
  AI_TICKET_REFUND: "AI 티켓 환불",
};

// 화면 표시용 부호 판단 (실사용 가능 캐시 기준 증가/감소) - 정확한 회계 처리는 백엔드 기준이며 여기선 표시 목적만
const NEGATIVE_TYPES = new Set([
  "CHALLENGE_HOLD",
  "CHALLENGE_PRINCIPAL_FAIL",
  "WITHDRAWAL",
  "AI_TICKET_PURCHASE",
]);

export const formatSignedAmount = (type, amount) => {
  const sign = NEGATIVE_TYPES.has(type) ? "-" : "+";
  return `${sign}${formatCurrency(Math.abs(amount ?? 0)).replace("₩ ", "₩")}`;
};

const TYPE_BADGE_VARIANTS = {
  CHARGE: "success",
  CHALLENGE_HOLD: "warning",
  WITHDRAWAL: "primary",
};

export const getTransactionBadge = (item) => {
  const typeLabel = TRANSACTION_TYPE_LABELS[item.type] ?? item.type;

  if (item.status === "FAILED" || item.status === "PENDING") {
    return { label: `${typeLabel} 실패`, variant: "danger" };
  }
  if (item.status === "PROCESSING") {
    return { label: `${typeLabel} 처리중`, variant: "warning" };
  }
  return { label: typeLabel, variant: TYPE_BADGE_VARIANTS[item.type] ?? "gray" };
};

export const formatDate = (isoString) => {
  if (!isoString) return "-";
  const date = new Date(isoString);
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};
