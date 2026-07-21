"use client";

import { useState } from "react";
import { Check, X } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { formatCurrency } from "@/features/wallet/format";

import { useCompleteWithdrawal, useRejectWithdrawal, useWithdrawalList } from "../hooks";

const STATUS_TABS = [
  { value: undefined, label: "전체" },
  { value: "PENDING", label: "대기중" },
  { value: "COMPLETED", label: "완료" },
  { value: "REJECTED", label: "반려됨" },
];

const STATUS_LABEL = {
  PENDING: "대기중",
  COMPLETED: "완료",
  REJECTED: "반려됨",
};

const SOURCE_LABEL = {
  CHARGED: "충전 캐시",
  REWARD: "리워드 캐시",
};

const formatDateTime = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

const WithdrawalRow = ({ item, onComplete, onReject, isPending }) => {
  const [isRejecting, setIsRejecting] = useState(false);
  const [isConfirmingComplete, setIsConfirmingComplete] = useState(false);
  const [reason, setReason] = useState("");

  const handleRejectConfirm = () => {
    if (!reason.trim()) return;
    onReject(item.id, reason.trim());
    setIsRejecting(false);
    setReason("");
  };

  const handleCompleteConfirm = () => {
    onComplete(item.id);
    setIsConfirmingComplete(false);
  };

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-600">
              {SOURCE_LABEL[item.sourceType] ?? item.sourceType}
            </span>
            <span className="text-xs text-gray-400">유저 ID {item.userId}</span>
          </div>
          <p className="text-lg font-bold text-gray-900">{formatCurrency(item.netTransferAmount)}</p>
          <p className="text-xs text-gray-500">
            신청 금액 {formatCurrency(item.amount)} · 수수료 {formatCurrency(item.feeAmount)}
          </p>
          <p className="text-sm text-gray-600">
            {item.bankName} {item.accountNumber} · {item.accountHolderName}
          </p>
          <p className="text-xs text-gray-400">
            신청일 {formatDateTime(item.requestedAt)}
            {item.processedAt && ` · 처리일 ${formatDateTime(item.processedAt)}`}
          </p>
          {item.rejectReason && <p className="text-xs text-danger">반려 사유: {item.rejectReason}</p>}
        </div>
        <span className="shrink-0 rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-600">
          {STATUS_LABEL[item.status] ?? item.status}
        </span>
      </div>

      {item.status === "PENDING" && !isRejecting && !isConfirmingComplete && (
        <div className="flex gap-2 border-t border-gray-100 pt-3">
          <Button onClick={() => setIsConfirmingComplete(true)} disabled={isPending}>
            <Check size={14} className="mr-1 inline" aria-hidden="true" />
            승인
          </Button>
          <Button variant="outline" onClick={() => setIsRejecting(true)} disabled={isPending}>
            <X size={14} className="mr-1 inline" aria-hidden="true" />
            반려
          </Button>
        </div>
      )}

      {isConfirmingComplete && (
        <div className="flex flex-col gap-2 border-t border-gray-100 pt-3">
          <p className="text-sm text-gray-600">
            {formatCurrency(item.netTransferAmount)} 출금을 승인 처리할까요? 실제 계좌 이체는 별도로 완료된 상태여야 해요.
          </p>
          <div className="flex gap-2">
            <Button onClick={handleCompleteConfirm} disabled={isPending}>
              승인 확정
            </Button>
            <Button variant="outline" onClick={() => setIsConfirmingComplete(false)}>
              취소
            </Button>
          </div>
        </div>
      )}

      {isRejecting && (
        <div className="flex flex-col gap-2 border-t border-gray-100 pt-3">
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="반려 사유를 입력해주세요."
            rows={2}
            aria-label="반려 사유"
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-primary"
          />
          <div className="flex gap-2">
            <Button variant="danger" onClick={handleRejectConfirm} disabled={!reason.trim() || isPending}>
              반려 확정
            </Button>
            <Button variant="outline" onClick={() => setIsRejecting(false)}>
              취소
            </Button>
          </div>
        </div>
      )}
    </Card>
  );
};

const WithdrawalAdminPage = () => {
  const [status, setStatus] = useState("PENDING");
  const { data, isLoading, isError, error } = useWithdrawalList(status);
  const completeMutation = useCompleteWithdrawal();
  const rejectMutation = useRejectWithdrawal();

  const items = data?.content ?? [];
  const isMutating = completeMutation.isPending || rejectMutation.isPending;

  const handleComplete = (withdrawalId) => {
    completeMutation.mutate(withdrawalId);
  };

  const handleReject = (withdrawalId, rejectReason) => {
    rejectMutation.mutate({ withdrawalId, rejectReason });
  };

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4">
      <h1 className="text-xl font-bold text-gray-900">출금 관리</h1>

      <div className="flex gap-2" role="group" aria-label="상태 필터">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            aria-pressed={status === tab.value}
            onClick={() => setStatus(tab.value)}
            className={`rounded-lg border px-3 py-1.5 text-sm font-semibold transition-colors ${
              status === tab.value
                ? "border-primary bg-primary text-white"
                : "border-gray-200 text-gray-600 hover:border-gray-300"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {(completeMutation.isError || rejectMutation.isError) && (
        <ErrorMessage error={completeMutation.error ?? rejectMutation.error} />
      )}

      {isLoading ? (
        <Loading />
      ) : isError ? (
        <ErrorMessage error={error} />
      ) : items.length === 0 ? (
        <Card>
          <p className="py-12 text-center text-sm text-gray-500">해당하는 출금 신청이 없어요.</p>
        </Card>
      ) : (
        <div className="flex flex-col gap-3">
          {items.map((item) => (
            <WithdrawalRow
              key={item.id}
              item={item}
              onComplete={handleComplete}
              onReject={handleReject}
              isPending={isMutating}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default WithdrawalAdminPage;
