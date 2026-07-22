"use client";

import { useEffect, useRef, useState } from "react";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";

const AiReviewRuleDialog = ({ initialRule, isPending, error, onClose, onSubmit }) => {
  const [ruleText, setRuleText] = useState(initialRule?.ruleText ?? "");
  const [reviewMode, setReviewMode] = useState(initialRule?.reviewMode ?? "AUTO");
  const [validationError, setValidationError] = useState("");
  const dialogRef = useRef(null);

  useEffect(() => {
    dialogRef.current?.focus();

    const handleKeyDown = (event) => {
      if (event.key === "Escape" && !isPending) onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isPending, onClose]);

  const submit = (event) => {
    event.preventDefault();
    const normalizedRule = ruleText.trim();
    if (!normalizedRule) {
      setValidationError("AI가 판단할 수 있도록 검수 기준을 입력해주세요.");
      return;
    }
    onSubmit({ ruleText: normalizedRule, reviewMode });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <form
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="ai-review-rule-dialog-title"
        tabIndex={-1}
        onSubmit={submit}
        className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-xl"
      >
        <h2 id="ai-review-rule-dialog-title" className="text-lg font-bold text-gray-900">
          AI 검수 기준 설정
        </h2>
        <p className="mt-2 text-sm text-gray-500">
          승인, 반려, 보류 조건을 AI가 구분할 수 있도록 구체적으로 작성해주세요.
        </p>

        <label htmlFor="ai-review-rule-text" className="mt-5 block text-sm font-semibold text-gray-800">
          검수 기준
        </label>
        <textarea
          id="ai-review-rule-text"
          value={ruleText}
          onChange={(event) => setRuleText(event.target.value)}
          disabled={isPending}
          rows={7}
          placeholder="예) 시작 시각과 종료 시각이 모두 보이고 30분 이상이면 승인합니다. 시각을 확인할 수 없으면 보류합니다."
          className="mt-2 w-full resize-none rounded-xl border border-gray-300 px-4 py-3 text-sm text-gray-800 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 disabled:bg-gray-50"
        />

        <fieldset className="mt-5" disabled={isPending}>
          <legend className="text-sm font-semibold text-gray-800">검수 방식</legend>
          <div className="mt-2 grid gap-3 sm:grid-cols-2">
            <label className="cursor-pointer rounded-xl border border-gray-200 p-4 has-checked:border-primary has-checked:bg-primary-light">
              <input
                type="radio"
                name="reviewMode"
                value="AUTO"
                checked={reviewMode === "AUTO"}
                onChange={(event) => setReviewMode(event.target.value)}
                className="mr-2"
              />
              <span className="text-sm font-semibold text-gray-800">자동 검수</span>
              <span className="mt-1 block text-xs text-gray-500">AI 결과를 인증글에 바로 반영해요.</span>
            </label>
            <label className="cursor-pointer rounded-xl border border-gray-200 p-4 has-checked:border-primary has-checked:bg-primary-light">
              <input
                type="radio"
                name="reviewMode"
                value="MANUAL"
                checked={reviewMode === "MANUAL"}
                onChange={(event) => setReviewMode(event.target.value)}
                className="mr-2"
              />
              <span className="text-sm font-semibold text-gray-800">검수 보조</span>
              <span className="mt-1 block text-xs text-gray-500">AI 결과를 참고해 방장이 결정해요.</span>
            </label>
          </div>
        </fieldset>

        {validationError && <p className="mt-3 text-sm text-danger">{validationError}</p>}
        {error && (
          <div className="mt-4">
            <ErrorMessage error={error} />
          </div>
        )}

        <div className="mt-6 flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
            취소
          </Button>
          <Button type="submit" disabled={isPending}>
            {isPending ? "저장 중..." : "저장하고 활성화"}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default AiReviewRuleDialog;
