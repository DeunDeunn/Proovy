"use client";

import { useState } from "react";
import Link from "next/link";
import { Check, X } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

import { useProcessReport, useRejectReport, useReportList } from "../hooks";

const TARGET_TABS = [
  { value: undefined, label: "전체" },
  { value: "POST", label: "게시글" },
  { value: "COMMENT", label: "댓글" },
];

const TARGET_LABEL = { POST: "게시글", COMMENT: "댓글" };

const REASON_LABEL = {
  SPAM: "스팸",
  ABUSE: "욕설",
  OBSCENE: "음란물",
  FALSE_CERT: "허위인증",
  ETC: "기타",
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

const ReportRow = ({ item, onProcess, onReject, isPending }) => {
  const [isConfirming, setIsConfirming] = useState(false);

  const handleProcessConfirm = () => {
    onProcess(item.id);
    setIsConfirming(false);
  };

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-600">
              {TARGET_LABEL[item.targetType] ?? item.targetType}
            </span>
            <span className="text-xs text-gray-400">대상 ID {item.targetId}</span>
            {item.targetType === "POST" && (
              <Link
                href={`/certification-posts/${item.targetId}`}
                className="text-xs text-primary hover:underline"
              >
                게시글 보기
              </Link>
            )}
          </div>
          <p className="text-sm font-semibold text-gray-900">
            {REASON_LABEL[item.reason] ?? item.reason}
          </p>
          {item.detail && <p className="text-sm text-gray-600">{item.detail}</p>}
          <p className="text-xs text-gray-400">
            신고자 {item.reporterNickname} · {formatDateTime(item.createdAt)}
          </p>
        </div>
      </div>

      {!isConfirming ? (
        <div className="flex gap-2 border-t border-gray-100 pt-3">
          <Button
            variant="danger"
            className="!border !border-red-500 !bg-transparent !text-red-500 hover:!bg-red-50"
            onClick={() => setIsConfirming(true)}
            disabled={isPending}
          >
            <Check size={14} className="mr-1 inline" aria-hidden="true" />
            신고 인정
          </Button>
          <Button variant="outline" onClick={() => onReject(item.id)} disabled={isPending}>
            <X size={14} className="mr-1 inline" aria-hidden="true" />
            기각
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-2 border-t border-gray-100 pt-3">
          <p className="text-sm text-gray-600">대상 콘텐츠가 삭제돼요. 정말 신고를 인정할까요?</p>
          <div className="flex gap-2">
            <Button
              variant="danger"
              className="!border !border-red-500 !bg-transparent !text-red-500 hover:!bg-red-50"
              onClick={handleProcessConfirm}
              disabled={isPending}
            >
              삭제 확정
            </Button>
            <Button variant="outline" onClick={() => setIsConfirming(false)}>
              취소
            </Button>
          </div>
        </div>
      )}
    </Card>
  );
};

const ReportAdminPage = () => {
  const [targetType, setTargetType] = useState(undefined);
  const { data, isLoading, isError, error } = useReportList(targetType);
  const processMutation = useProcessReport();
  const rejectMutation = useRejectReport();

  const items = data?.items ?? [];
  const isMutating = processMutation.isPending || rejectMutation.isPending;

  const handleProcess = (reportId) => {
    processMutation.mutate(reportId);
  };

  const handleReject = (reportId) => {
    rejectMutation.mutate(reportId);
  };

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4">
      <div>
        <h1 className="text-xl font-bold text-gray-900">신고 관리</h1>
        <p className="mt-1 text-sm text-gray-500">처리 대기 중인 신고만 표시돼요.</p>
      </div>

      <div className="flex gap-2" role="group" aria-label="대상 필터">
        {TARGET_TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            aria-pressed={targetType === tab.value}
            onClick={() => setTargetType(tab.value)}
            className={`rounded-lg border px-3 py-1.5 text-sm font-semibold transition-colors ${
              targetType === tab.value
                ? "border-primary bg-primary text-white"
                : "border-gray-200 text-gray-600 hover:border-gray-300"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {(processMutation.isError || rejectMutation.isError) && (
        <ErrorMessage error={processMutation.error ?? rejectMutation.error} />
      )}

      {isLoading ? (
        <Loading />
      ) : isError ? (
        <ErrorMessage error={error} />
      ) : items.length === 0 ? (
        <Card>
          <p className="py-12 text-center text-sm text-gray-500">대기 중인 신고가 없어요.</p>
        </Card>
      ) : (
        <div className="flex flex-col gap-3">
          {items.map((item) => (
            <ReportRow
              key={item.id}
              item={item}
              onProcess={handleProcess}
              onReject={handleReject}
              isPending={isMutating}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default ReportAdminPage;
