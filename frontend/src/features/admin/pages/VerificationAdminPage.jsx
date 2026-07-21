"use client";

import { useState } from "react";
import { Check, X } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

import { useReviewVerification, useVerificationList } from "../hooks";

const STATUS_TABS = [
  { value: undefined, label: "전체" },
  { value: "PENDING", label: "대기중" },
  { value: "APPROVED", label: "승인됨" },
  { value: "REJECTED", label: "반려됨" },
  { value: "REVOKED", label: "취소됨" },
];

const STATUS_LABEL = {
  PENDING: "대기중",
  APPROVED: "승인됨",
  REJECTED: "반려됨",
  REVOKED: "취소됨",
};

const formatDate = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}.${m}.${d}`;
};

const VerificationRow = ({ item, onApprove, onReject, isPending }) => {
  const [isRejecting, setIsRejecting] = useState(false);
  const [reason, setReason] = useState("");

  const handleRejectConfirm = () => {
    if (!reason.trim()) return;
    onReject(item.id, reason.trim());
    setIsRejecting(false);
    setReason("");
  };

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-semibold text-gray-900">{item.nickname}</p>
          <p className="text-xs text-gray-400">신청일 {formatDate(item.appliedAt)}</p>
        </div>
        <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-600">
          {STATUS_LABEL[item.status] ?? item.status}
        </span>
      </div>

      {item.status === "PENDING" && !isRejecting && (
        <div className="flex gap-2">
          <Button onClick={() => onApprove(item.id)} disabled={isPending}>
            <Check size={14} className="mr-1 inline" aria-hidden="true" />
            승인
          </Button>
          <Button variant="outline" onClick={() => setIsRejecting(true)} disabled={isPending}>
            <X size={14} className="mr-1 inline" aria-hidden="true" />
            반려
          </Button>
        </div>
      )}

      {isRejecting && (
        <div className="flex flex-col gap-2">
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

const VerificationAdminPage = () => {
  const [status, setStatus] = useState("PENDING");
  const { data, isLoading, isError, error } = useVerificationList(status);
  const reviewMutation = useReviewVerification();

  const items = data?.content ?? [];

  const handleApprove = (id) => {
    reviewMutation.mutate({ id, status: "APPROVED" });
  };

  const handleReject = (id, rejectionReason) => {
    reviewMutation.mutate({ id, status: "REJECTED", rejectionReason });
  };

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4">
      <h1 className="text-xl font-bold text-gray-900">우수 사용자 인증 관리</h1>

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

      {reviewMutation.isError && <ErrorMessage error={reviewMutation.error} />}

      {isLoading ? (
        <Loading />
      ) : isError ? (
        <ErrorMessage error={error} />
      ) : items.length === 0 ? (
        <Card>
          <p className="py-12 text-center text-sm text-gray-500">해당하는 신청 내역이 없어요.</p>
        </Card>
      ) : (
        <div className="flex flex-col gap-3">
          {items.map((item) => (
            <VerificationRow
              key={item.id}
              item={item}
              onApprove={handleApprove}
              onReject={handleReject}
              isPending={reviewMutation.isPending}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default VerificationAdminPage;
