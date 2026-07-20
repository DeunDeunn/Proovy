"use client";

import { Award } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

import { useApplyVerification, useVerificationStatus } from "../hooks";

const formatDate = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}.${m}.${d}`;
};

const STATUS_META = {
  PENDING: {
    title: "심사 중이에요",
    desc: (data) => `${formatDate(data.appliedAt)}에 신청하셨어요. 결과를 기다려주세요.`,
  },
  APPROVED: {
    title: "우수 사용자로 인증됐어요",
    desc: (data) => `${formatDate(data.approvedAt)}에 승인됐어요.`,
  },
  REJECTED: {
    title: "신청이 반려됐어요",
    desc: (data) => data.rejectionReason || "반려 사유가 등록되지 않았어요.",
  },
  REVOKED: {
    title: "인증이 취소됐어요",
    desc: () => "다시 신청하실 수 있어요.",
  },
};

const CAN_APPLY_STATUSES = new Set([null, "REJECTED", "REVOKED"]);

const VerificationPage = () => {
  const { data, isLoading, isError, error } = useVerificationStatus();
  const applyVerification = useApplyVerification();

  if (isLoading) return <Loading />;
  if (isError) return <ErrorMessage error={error} />;
  if (!data) return null;

  const meta = data.status ? STATUS_META[data.status] : null;
  const isStatusApplicable = CAN_APPLY_STATUSES.has(data.status);
  const isEligible = data.successCount >= data.requiredCount;
  const canApply = isStatusApplicable && isEligible;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-2">
        <h1 className="text-xl font-bold text-gray-900">우수 사용자 인증</h1>
        <p className="text-sm text-gray-500">
          성공한 챌린지가 {data.requiredCount}개 이상이면 신청할 수 있어요. (현재 {data.successCount}/
          {data.requiredCount}개)
        </p>
      </div>

      <Card className="flex flex-col items-center gap-4 py-10 text-center">
        <Award size={40} className="text-amber-500" />

        {meta ? (
          <div className="flex flex-col gap-1">
            <p className="text-base font-semibold text-gray-900">{meta.title}</p>
            <p className="text-sm text-gray-500">{meta.desc(data)}</p>
          </div>
        ) : (
          <p className="text-sm text-gray-500">아직 신청 내역이 없어요.</p>
        )}

        {isStatusApplicable && (
          <div className="flex flex-col items-center gap-2">
            <Button onClick={() => applyVerification.mutate()} disabled={!canApply || applyVerification.isPending}>
              {applyVerification.isPending ? "신청 중..." : data.status ? "다시 신청하기" : "신청하기"}
            </Button>
            {!isEligible && (
              <p className="text-xs text-gray-400">
                성공한 챌린지 {data.requiredCount - data.successCount}개 더 필요해요.
              </p>
            )}
          </div>
        )}

        {applyVerification.isError && <ErrorMessage error={applyVerification.error} />}
      </Card>
    </div>
  );
};

export default VerificationPage;
