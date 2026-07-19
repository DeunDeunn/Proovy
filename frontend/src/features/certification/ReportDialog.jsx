"use client";

import { useState } from "react";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";

import { useCreateReport } from "./hooks";

const reportReasons = [
  { value: "SPAM", label: "스팸" },
  { value: "ABUSE", label: "욕설" },
  { value: "OBSCENE", label: "음란물" },
  { value: "FALSE_CERT", label: "허위 인증" },
  { value: "ETC", label: "기타" },
];

const ReportDialog = ({ targetType, targetId, onClose }) => {
  const [reason, setReason] = useState("");
  const [detail, setDetail] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const createReportMutation = useCreateReport();

  const submitReport = (event) => {
    event.preventDefault();
    if (!reason || createReportMutation.isPending) return;

    createReportMutation.mutate(
      {
        targetType,
        targetId,
        reason,
        detail: detail.trim() || null,
      },
      {
        onSuccess: () => setSubmitted(true),
      }
    );
  };

  return (
    <div
      role="presentation"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4"
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="report-dialog-title"
        className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl"
      >
        {submitted ? (
          <div>
            <h2 id="report-dialog-title" className="text-lg font-bold text-gray-900">
              신고가 접수됐어요
            </h2>
            <p className="mt-2 text-sm leading-6 text-gray-600">
              운영자가 내용을 확인한 뒤 처리할 예정입니다.
            </p>
            <div className="mt-6 flex justify-end">
              <Button type="button" onClick={onClose}>
                확인
              </Button>
            </div>
          </div>
        ) : (
          <form onSubmit={submitReport}>
            <h2 id="report-dialog-title" className="text-lg font-bold text-gray-900">
              신고하기
            </h2>
            <p className="mt-2 text-sm leading-6 text-gray-600">
              신고 사유를 선택해주세요. 신고 내용은 운영자 검토에만 사용됩니다.
            </p>

            <fieldset className="mt-5">
              <legend className="text-sm font-semibold text-gray-800">신고 사유</legend>
              <div className="mt-3 grid grid-cols-2 gap-2">
                {reportReasons.map((item) => (
                  <label
                    key={item.value}
                    className={`cursor-pointer rounded-lg border px-3 py-2.5 text-sm font-medium transition-colors ${
                      reason === item.value
                        ? "border-primary bg-primary-light text-primary"
                        : "border-gray-200 text-gray-700 hover:bg-gray-50"
                    }`}
                  >
                    <input
                      type="radio"
                      name={`report-reason-${targetType}-${targetId}`}
                      value={item.value}
                      checked={reason === item.value}
                      onChange={(event) => setReason(event.target.value)}
                      className="sr-only"
                    />
                    {item.label}
                  </label>
                ))}
              </div>
            </fieldset>

            <div className="mt-5">
              <label
                htmlFor={`report-detail-${targetType}-${targetId}`}
                className="text-sm font-semibold text-gray-800"
              >
                상세 사유 <span className="font-normal text-gray-400">(선택)</span>
              </label>
              <textarea
                id={`report-detail-${targetType}-${targetId}`}
                value={detail}
                onChange={(event) => setDetail(event.target.value)}
                maxLength={500}
                rows={4}
                placeholder="신고 사유를 자세히 알려주세요."
                disabled={createReportMutation.isPending}
                className="mt-2 w-full resize-y rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-gray-50"
              />
              <p className="mt-1 text-right text-xs text-gray-400">{detail.length} / 500</p>
            </div>

            {createReportMutation.error && (
              <div className="mt-4">
                <ErrorMessage error={createReportMutation.error} />
              </div>
            )}

            <div className="mt-6 flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={createReportMutation.isPending}
              >
                취소
              </Button>
              <Button type="submit" disabled={!reason || createReportMutation.isPending}>
                {createReportMutation.isPending ? "접수 중..." : "신고 등록"}
              </Button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
};

export default ReportDialog;
