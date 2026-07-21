"use client";

/* eslint-disable @next/next/no-img-element -- blob: 미리보기 URL은 next/image 설정 대상이 아니다. */

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Camera } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import { useCategories, useCreateChallenge, useUpdateChallengeThumbnail } from "./hooks";

const DESCRIPTION_MAX_LENGTH = 500;
const CERT_TIME_MIN = "02:00";
const CERT_TIME_MAX = "23:00";

const inputClassName =
  "w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-primary";
const labelClassName = "mb-1 block text-xs text-gray-500";

const STEPS = [
  { step: 1, label: "기본 설정" },
  { step: 2, label: "상세 설정" },
  { step: 3, label: "인증 설정" },
  { step: 4, label: "확인" },
];

const initialForm = {
  title: "",
  categoryId: "",
  entryFee: 10000,
  description: "",
  startDate: "",
  endDate: "",
  maxParticipants: 10,
  verificationMethod: "",
  dailyCertLimit: 1,
  certStartTime: "06:00",
  certEndTime: "22:00",
  aiReviewEnabled: false,
  feedVisibility: "PUBLIC",
};

const SummaryRow = ({ label, value }) => (
  <div className="flex items-center justify-between py-2 text-sm">
    <span className="text-gray-500">{label}</span>
    <span className="font-medium text-gray-800">{value}</span>
  </div>
);

const Stepper = ({ current }) => (
  <div className="flex items-center justify-center">
    {STEPS.map(({ step, label }, i) => (
      <div key={step} className="flex items-center">
        <div className="flex flex-col items-center gap-1">
          <div
            className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold ${
              current >= step ? "bg-primary text-white" : "border border-gray-300 text-gray-400"
            }`}
          >
            {step}
          </div>
          <span
            className={`text-xs whitespace-nowrap ${
              current === step ? "font-semibold text-gray-900" : "text-gray-400"
            }`}
          >
            {label}
          </span>
        </div>
        {i < STEPS.length - 1 && (
          <div
            className={`mx-2 mb-4 h-px w-10 sm:w-16 ${current > step ? "bg-primary" : "bg-gray-200"}`}
          />
        )}
      </div>
    ))}
  </div>
);

const ChallengeCreatePage = () => {
  const router = useRouter();
  const { data: categories, isError: isCategoriesError, error: categoriesError } = useCategories();
  const createMutation = useCreateChallenge();
  const thumbnailMutation = useUpdateChallengeThumbnail();
  const [step, setStep] = useState(1);
  const [form, setForm] = useState(initialForm);
  const [justChangedStep, setJustChangedStep] = useState(false);
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [thumbnailPreview, setThumbnailPreview] = useState(null);

  const handleThumbnailChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setThumbnailFile(file);
    setThumbnailPreview(URL.createObjectURL(file));
  };

  const setField = (field) => (e) => {
    const { value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [field]: type === "checkbox" ? checked : value }));
  };

  const isStep1Valid =
    form.title.trim() !== "" && form.categoryId !== "" && Number(form.entryFee) >= 1000;
  const isStep2Valid =
    form.startDate !== "" &&
    form.endDate !== "" &&
    form.endDate > form.startDate &&
    Number(form.maxParticipants) > 0;
  const isStep3Valid =
    form.verificationMethod.trim() !== "" &&
    form.certEndTime > form.certStartTime &&
    form.certStartTime >= CERT_TIME_MIN &&
    form.certEndTime <= CERT_TIME_MAX;
  const isFormValid = isStep1Valid && isStep2Valid && isStep3Valid;

  const periodDays =
    form.startDate && form.endDate && form.endDate > form.startDate
      ? Math.round((new Date(form.endDate) - new Date(form.startDate)) / (1000 * 60 * 60 * 24)) + 1
      : null;

  const selectedCategoryName = categories?.find(
    (category) => String(category.id) === String(form.categoryId)
  )?.name;

  // 다음 단계로 넘어가면 같은 자리의 버튼이 곧바로 "챌린지 개설하기" 제출 버튼으로 바뀌기 때문에,
  // 빠르게 두 번 누르면 확인 단계를 보기도 전에 바로 제출돼버린다 — 전환 직후 잠깐 눌러도 안 먹게 막는다
  const goNext = () => {
    setStep((prev) => Math.min(prev + 1, STEPS.length));
    setJustChangedStep(true);
    setTimeout(() => setJustChangedStep(false), 400);
  };
  const goBack = () => setStep((prev) => Math.max(prev - 1, 1));

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!isFormValid) return;

    createMutation.mutate(
      {
        title: form.title,
        categoryId: Number(form.categoryId),
        entryFee: Number(form.entryFee),
        description: form.description,
        startDate: form.startDate,
        endDate: form.endDate,
        maxParticipants: Number(form.maxParticipants),
        verificationMethod: form.verificationMethod,
        dailyCertLimit: Number(form.dailyCertLimit),
        certStartTime: form.certStartTime,
        certEndTime: form.certEndTime,
        aiReviewEnabled: form.aiReviewEnabled,
        feedVisibility: form.feedVisibility,
      },
      {
        onSuccess: async (created) => {
          if (thumbnailFile) {
            try {
              await thumbnailMutation.mutateAsync({ challengeId: created.id, file: thumbnailFile });
            } catch {
              // 사진 업로드가 실패해도 챌린지 자체는 이미 만들어졌으니 상세 페이지로 이동시키고,
              // 사진은 상세 페이지의 "사진 변경"으로 나중에 다시 시도할 수 있다
            }
          }
          router.push(`/challenges/${created.id}`);
        },
      }
    );
  };

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-[640px] space-y-6 pb-2">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">챌린지 개설하기</h1>
        <p className="mt-2 text-sm text-gray-500">매력적인 챌린지를 만들어 보세요!</p>
      </div>

      <Stepper current={step} />

      {step === 1 && (
        <Card>
          <h2 className="mb-4 text-base font-bold text-gray-900">기본 설정</h2>
          <div className="space-y-4">
            <div>
              <label htmlFor="create-title" className={labelClassName}>
                챌린지 제목
              </label>
              <input
                id="create-title"
                type="text"
                placeholder="예) 매일 30분 러닝"
                value={form.title}
                onChange={setField("title")}
                className={inputClassName}
              />
            </div>

            <div>
              <label htmlFor="create-category" className={labelClassName}>
                카테고리
              </label>
              {isCategoriesError ? (
                <ErrorMessage error={categoriesError} />
              ) : (
                <select
                  id="create-category"
                  value={form.categoryId}
                  onChange={setField("categoryId")}
                  className={inputClassName}
                >
                  <option value="">선택하세요</option>
                  {categories?.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label htmlFor="create-entry-fee" className={labelClassName}>
                참가비 (1,000원 이상)
              </label>
              <input
                id="create-entry-fee"
                type="number"
                min={1000}
                step={1000}
                value={form.entryFee}
                onChange={setField("entryFee")}
                className={inputClassName}
              />
            </div>

            <div>
              <label className={labelClassName}>챌린지 사진 (선택)</label>
              <label className="flex h-28 cursor-pointer items-center justify-center overflow-hidden rounded-lg border border-dashed border-gray-300 text-gray-400 hover:bg-gray-50">
                {thumbnailPreview ? (
                  <img
                    src={thumbnailPreview}
                    alt="챌린지 사진 미리보기"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <span className="flex flex-col items-center gap-1 text-xs">
                    <Camera size={20} />
                    사진 선택
                  </span>
                )}
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  className="hidden"
                  onChange={handleThumbnailChange}
                />
              </label>
              <p className="mt-1 text-xs text-gray-400">
                지금 선택하지 않아도 개설 후 상세 페이지에서 언제든 추가할 수 있어요.
              </p>
            </div>
          </div>
        </Card>
      )}

      {step === 2 && (
        <Card>
          <h2 className="mb-4 text-base font-bold text-gray-900">상세 설정</h2>
          <div className="space-y-4">
            <div>
              <label htmlFor="create-description" className={labelClassName}>
                챌린지 설명
              </label>
              <textarea
                id="create-description"
                rows={3}
                maxLength={DESCRIPTION_MAX_LENGTH}
                placeholder="챌린지에 대해 자세히 소개해주세요."
                value={form.description}
                onChange={setField("description")}
                className={`${inputClassName} resize-none`}
              />
              <p className="mt-1 text-right text-xs text-gray-400">
                {form.description.length} / {DESCRIPTION_MAX_LENGTH}
              </p>
            </div>

            <div>
              <label className={labelClassName}>진행 기간</label>
              <div className="flex items-center gap-2">
                <input
                  aria-label="시작일"
                  type="date"
                  value={form.startDate}
                  onChange={setField("startDate")}
                  className={`${inputClassName} min-w-0`}
                />
                <span className="shrink-0 text-gray-400">~</span>
                <input
                  aria-label="종료일"
                  type="date"
                  value={form.endDate}
                  onChange={setField("endDate")}
                  className={`${inputClassName} min-w-0`}
                />
              </div>
              {periodDays && (
                <p className="mt-1 text-xs text-gray-400">총 {periodDays}일간 진행돼요.</p>
              )}
              {form.startDate && form.endDate && form.endDate <= form.startDate && (
                <p className="mt-1 text-xs text-danger">종료일은 시작일보다 나중이어야 해요.</p>
              )}
            </div>

            <div>
              <label htmlFor="create-max-participants" className={labelClassName}>
                모집 정원
              </label>
              <input
                id="create-max-participants"
                type="number"
                min={1}
                value={form.maxParticipants}
                onChange={setField("maxParticipants")}
                className={inputClassName}
              />
            </div>
          </div>
        </Card>
      )}

      {step === 3 && (
        <Card>
          <h2 className="mb-4 text-base font-bold text-gray-900">인증 설정</h2>
          <div className="space-y-4">
            <div>
              <label htmlFor="create-verification-method" className={labelClassName}>
                인증 방법
              </label>
              <input
                id="create-verification-method"
                type="text"
                placeholder="예) 러닝 앱 캡처 또는 인증 사진"
                value={form.verificationMethod}
                onChange={setField("verificationMethod")}
                className={inputClassName}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="create-daily-cert-limit" className={labelClassName}>
                  하루 인증 횟수
                </label>
                <select
                  id="create-daily-cert-limit"
                  value={form.dailyCertLimit}
                  onChange={setField("dailyCertLimit")}
                  className={inputClassName}
                >
                  <option value={1}>1회</option>
                  <option value={2}>2회</option>
                  <option value={3}>3회</option>
                </select>
              </div>
              <div>
                <label className={labelClassName}>인증 가능 시간</label>
                <div className="flex items-center gap-2">
                  <input
                    type="time"
                    min={CERT_TIME_MIN}
                    max={CERT_TIME_MAX}
                    value={form.certStartTime}
                    onChange={setField("certStartTime")}
                    className={`${inputClassName} min-w-0`}
                  />
                  <span className="shrink-0 text-gray-400">~</span>
                  <input
                    type="time"
                    min={CERT_TIME_MIN}
                    max={CERT_TIME_MAX}
                    value={form.certEndTime}
                    onChange={setField("certEndTime")}
                    className={`${inputClassName} min-w-0`}
                  />
                </div>
              </div>
            </div>
            {form.certEndTime <= form.certStartTime && (
              <p className="text-xs text-danger">인증 종료 시간은 시작 시간보다 나중이어야 해요.</p>
            )}
            {(form.certStartTime < CERT_TIME_MIN || form.certEndTime > CERT_TIME_MAX) && (
              <p className="text-xs text-danger">
                인증 가능 시간은 오전 2시 ~ 오후 11시 사이로 설정해주세요.
              </p>
            )}

            <div className="flex items-center justify-between rounded-lg bg-gray-50 px-3.5 py-3">
              <div>
                <p className="text-sm font-medium text-gray-800">AI 자동검수 사용</p>
                <p className="mt-0.5 text-xs text-gray-400">AI가 인증 사진을 자동으로 검수해요.</p>
              </div>
              <label className="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  checked={form.aiReviewEnabled}
                  onChange={setField("aiReviewEnabled")}
                  className="peer sr-only"
                />
                <span className="h-6 w-11 rounded-full bg-gray-300 transition-colors peer-checked:bg-primary" />
                <span className="absolute left-1 h-4 w-4 rounded-full bg-white transition-transform peer-checked:translate-x-5" />
              </label>
            </div>

            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-500">성공 기준</span>
              <span className="font-medium text-gray-800">80% 이상 인증 시 성공 (고정)</span>
            </div>

            <div>
              <label htmlFor="create-feed-visibility" className={labelClassName}>
                피드 공개 범위
              </label>
              <select
                id="create-feed-visibility"
                value={form.feedVisibility}
                onChange={setField("feedVisibility")}
                className={inputClassName}
              >
                <option value="PUBLIC">전체 공개</option>
                <option value="PARTICIPANTS_ONLY">참가자만 공개</option>
              </select>
            </div>
          </div>
        </Card>
      )}

      {step === 4 && (
        <Card>
          <h2 className="mb-2 text-base font-bold text-gray-900">확인</h2>
          <p className="mb-2 text-xs text-gray-400">아래 내용으로 챌린지를 개설해요.</p>
          <div className="divide-y divide-gray-100">
            <SummaryRow label="제목" value={form.title} />
            <SummaryRow label="카테고리" value={selectedCategoryName ?? "-"} />
            <SummaryRow label="챌린지 사진" value={thumbnailFile ? "선택됨" : "선택 안 함"} />
            <SummaryRow label="참가비" value={`₩ ${Number(form.entryFee).toLocaleString()}`} />
            <SummaryRow
              label="진행 기간"
              value={periodDays ? `${form.startDate} ~ ${form.endDate} (${periodDays}일)` : "-"}
            />
            <SummaryRow label="모집 정원" value={`${form.maxParticipants}명`} />
            <SummaryRow label="인증 방법" value={form.verificationMethod || "-"} />
            <SummaryRow label="하루 인증 횟수" value={`${form.dailyCertLimit}회`} />
            <SummaryRow
              label="인증 가능 시간"
              value={`${form.certStartTime} ~ ${form.certEndTime}`}
            />
            <SummaryRow label="AI 자동검수" value={form.aiReviewEnabled ? "사용" : "미사용"} />
            <SummaryRow label="성공 기준" value="80% (고정)" />
            <SummaryRow
              label="피드 공개 범위"
              value={form.feedVisibility === "PUBLIC" ? "전체 공개" : "참가자만 공개"}
            />
          </div>
        </Card>
      )}

      {createMutation.isError && <ErrorMessage error={createMutation.error} />}

      <div className="flex gap-3">
        {step === 1 ? (
          <Button
            type="button"
            variant="outline"
            className="flex-1"
            onClick={() => router.push("/challenges")}
          >
            취소
          </Button>
        ) : (
          <Button type="button" variant="outline" className="flex-1" onClick={goBack}>
            이전 단계
          </Button>
        )}

        {step < STEPS.length ? (
          <Button
            type="button"
            className="flex-1"
            disabled={
              justChangedStep ||
              (step === 1 && !isStep1Valid) ||
              (step === 2 && !isStep2Valid) ||
              (step === 3 && !isStep3Valid)
            }
            onClick={goNext}
          >
            다음 단계
          </Button>
        ) : (
          <Button
            type="submit"
            className="flex-1"
            disabled={justChangedStep || !isFormValid || createMutation.isPending}
          >
            {createMutation.isPending ? "개설하는 중..." : "챌린지 개설하기"}
          </Button>
        )}
      </div>
    </form>
  );
};

export default ChallengeCreatePage;
