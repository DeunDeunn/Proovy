"use client";

/* eslint-disable @next/next/no-img-element -- 선택 직후의 blob: URL 미리보기에는 next/image 최적화를 적용할 수 없다. */

import { ImagePlus, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useChallenge } from "@/features/challenge/hooks";
import { formatChallengePeriod } from "@/lib/date";

import { useCreateCertificationPost } from "./hooks";

const MAX_IMAGE_SIZE = 10 * 1024 * 1024;
const MAX_ADDITIONAL_IMAGES = 3;
const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

const getFileError = (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return "JPEG, PNG, WebP 이미지 파일만 첨부할 수 있습니다.";
  }

  if (file.size > MAX_IMAGE_SIZE) {
    return "이미지 파일은 한 장당 10MB 이하만 첨부할 수 있습니다.";
  }

  return null;
};

const CertificationPostCreatePage = ({ challengeId }) => {
  const router = useRouter();
  const [contents, setContents] = useState("");
  const [thumbnail, setThumbnail] = useState(null);
  const [thumbnailPreview, setThumbnailPreview] = useState(null);
  const [additionalImages, setAdditionalImages] = useState([]);
  const [validationError, setValidationError] = useState(null);
  const thumbnailInputRef = useRef(null);
  const additionalPreviewsRef = useRef(new Set());

  const {
    data: challenge,
    isLoading: isChallengeLoading,
    error: challengeError,
  } = useChallenge(challengeId);
  const createMutation = useCreateCertificationPost(challengeId);

  useEffect(
    () => () => {
      if (thumbnailPreview) URL.revokeObjectURL(thumbnailPreview);
    },
    [thumbnailPreview]
  );

  useEffect(
    () => () => {
      additionalPreviewsRef.current.forEach((preview) => URL.revokeObjectURL(preview));
    },
    []
  );

  const handleThumbnailChange = (event) => {
    const [file] = event.target.files ?? [];
    if (!file) return;

    const fileError = getFileError(file);
    if (fileError) {
      setValidationError(fileError);
      event.target.value = "";
      return;
    }

    setValidationError(null);
    setThumbnail(file);
    setThumbnailPreview(URL.createObjectURL(file));
    event.target.value = "";
  };

  const handleAdditionalImagesChange = (event) => {
    const selectedFiles = Array.from(event.target.files ?? []);
    const remainingCount = MAX_ADDITIONAL_IMAGES - additionalImages.length;

    if (selectedFiles.length > remainingCount) {
      setValidationError(`추가 이미지는 최대 ${MAX_ADDITIONAL_IMAGES}장까지 첨부할 수 있습니다.`);
      event.target.value = "";
      return;
    }

    const invalidFile = selectedFiles.find((file) => getFileError(file));
    if (invalidFile) {
      setValidationError(getFileError(invalidFile));
      event.target.value = "";
      return;
    }

    const newImages = selectedFiles.map((file) => ({
      file,
      preview: URL.createObjectURL(file),
    }));

    newImages.forEach((image) => additionalPreviewsRef.current.add(image.preview));
    setValidationError(null);
    setAdditionalImages((currentImages) => [...currentImages, ...newImages]);
    event.target.value = "";
  };

  const removeAdditionalImage = (index) => {
    setAdditionalImages((currentImages) => {
      const target = currentImages[index];
      if (target) {
        URL.revokeObjectURL(target.preview);
        additionalPreviewsRef.current.delete(target.preview);
      }
      return currentImages.filter((_, imageIndex) => imageIndex !== index);
    });
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    if (!thumbnail) {
      setValidationError("대표 인증 이미지를 한 장 첨부해주세요.");
      return;
    }

    setValidationError(null);
    createMutation.mutate(
      {
        contents: contents.trim(),
        thumbnail,
        images: additionalImages.map((image) => image.file),
      },
      {
        onSuccess: () => router.push(`/challenges/${challengeId}/feed`),
      }
    );
  };

  if (isChallengeLoading) return <Loading label="챌린지 정보를 불러오는 중..." />;
  if (challengeError) return <ErrorMessage error={challengeError} />;

  return (
    <div className="mx-auto max-w-6xl">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">인증글 작성</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid gap-8 lg:grid-cols-2">
          <section>
            <h2 className="mb-4 font-semibold text-gray-900">인증 이미지</h2>
            <input
              ref={thumbnailInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={handleThumbnailChange}
              className="sr-only"
              disabled={createMutation.isPending}
            />

            {thumbnailPreview ? (
              <div className="relative aspect-video overflow-hidden rounded-xl bg-gray-100">
                <img
                  src={thumbnailPreview}
                  alt="인증 이미지 미리보기"
                  className="h-full w-full object-cover"
                />
                <button
                  type="button"
                  onClick={() => thumbnailInputRef.current?.click()}
                  disabled={createMutation.isPending}
                  className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-black/60 px-4 py-2 text-sm font-medium text-white hover:bg-black/75 disabled:opacity-50"
                >
                  사진 변경
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setThumbnail(null);
                    setThumbnailPreview(null);
                  }}
                  aria-label="인증 이미지 제거"
                  disabled={createMutation.isPending}
                  className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/75 disabled:opacity-50"
                >
                  <X size={18} />
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => thumbnailInputRef.current?.click()}
                disabled={createMutation.isPending}
                className="flex aspect-video w-full flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-200 bg-gray-50 px-6 text-center transition-colors hover:border-primary hover:bg-primary-light disabled:cursor-not-allowed disabled:opacity-50"
              >
                <ImagePlus size={30} className="mb-3 text-gray-400" />
                <span className="text-sm font-medium text-gray-700">인증 이미지 선택</span>
                <span className="mt-1 text-xs text-gray-400">JPEG, PNG, WebP · 최대 10MB</span>
              </button>
            )}
          </section>

          <section>
            <label htmlFor="certification-contents" className="block font-semibold text-gray-900">
              본문
            </label>
            <div className="mt-4 aspect-video min-h-72 lg:min-h-0">
              <textarea
                id="certification-contents"
                value={contents}
                onChange={(event) => setContents(event.target.value)}
                placeholder="오늘의 인증 과정을 자유롭게 남겨주세요."
                maxLength={500}
                className="h-full w-full resize-none rounded-xl border border-gray-200 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
                disabled={createMutation.isPending}
              />
            </div>
          </section>

          <section className="rounded-xl border border-gray-200 bg-surface p-5">
            <h2 className="mb-4 font-semibold text-gray-900">추가 사진 (선택, 최대 3장)</h2>
            <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
              {additionalImages.map((image, index) => (
                <div
                  key={image.preview}
                  className="relative aspect-square overflow-hidden rounded-xl bg-gray-100"
                >
                  <img
                    src={image.preview}
                    alt={`추가 이미지 ${index + 1} 미리보기`}
                    className="h-full w-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => removeAdditionalImage(index)}
                    aria-label={`추가 이미지 ${index + 1} 제거`}
                    disabled={createMutation.isPending}
                    className="absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/75 disabled:opacity-50"
                  >
                    <X size={16} />
                  </button>
                </div>
              ))}

              {additionalImages.length < MAX_ADDITIONAL_IMAGES && (
                <label className="flex aspect-square cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-200 bg-gray-50 text-center transition-colors hover:border-primary hover:bg-primary-light">
                  <ImagePlus size={24} className="text-gray-400" />
                  <span className="mt-2 text-xs font-medium text-gray-600">이미지 추가</span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    multiple
                    onChange={handleAdditionalImagesChange}
                    className="sr-only"
                    disabled={createMutation.isPending}
                  />
                </label>
              )}
            </div>
          </section>

          <section className="h-full rounded-xl border border-gray-200 bg-surface p-4">
            <div className="flex gap-4">
              {challenge?.thumbnailUrl ? (
                <img
                  src={challenge.thumbnailUrl}
                  alt={`${challenge.title} 챌린지 이미지`}
                  className="h-20 w-28 shrink-0 rounded-lg object-cover"
                />
              ) : (
                <div className="flex h-20 w-28 shrink-0 items-center justify-center rounded-lg bg-gray-100 text-xs text-gray-400">
                  이미지 없음
                </div>
              )}
              <div className="min-w-0">
                <div className="flex items-start gap-2">
                  <p className="line-clamp-2 text-lg font-bold leading-6 text-gray-900">
                    {challenge?.title ?? `챌린지 #${challengeId}`}
                  </p>
                  <span className="shrink-0 rounded-full bg-primary-light px-2 py-0.5 text-xs font-semibold text-primary">
                    {challenge?.categoryName ?? "카테고리 없음"}
                  </span>
                </div>
              </div>
            </div>

            <dl className="mt-4 space-y-2 border-t border-gray-100 pt-3 text-sm">
              <div className="flex items-center justify-between gap-4">
                <dt className="text-gray-400">기간</dt>
                <dd className="text-right font-medium text-gray-700">
                  {formatChallengePeriod(challenge?.startDate, challenge?.endDate) || "정보 없음"}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-gray-400">인증 가능 시간</dt>
                <dd className="text-right font-medium text-gray-700">
                  {challenge?.certStartTime && challenge?.certEndTime
                    ? `${challenge.certStartTime} ~ ${challenge.certEndTime}`
                    : "정보 없음"}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-gray-400">성공 기준</dt>
                <dd className="text-right font-medium text-gray-700">
                  {challenge?.successCriteriaRate != null
                    ? `${challenge.successCriteriaRate}% 이상`
                    : "정보 없음"}
                </dd>
              </div>
            </dl>
          </section>
        </div>

        {(validationError || createMutation.isError) && (
          <ErrorMessage
            error={validationError ? { message: validationError } : createMutation.error}
          />
        )}

        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            className="!rounded-full"
            onClick={() => router.back()}
            disabled={createMutation.isPending}
          >
            취소
          </Button>
          <Button
            type="submit"
            className="min-w-28 !rounded-full"
            disabled={createMutation.isPending}
          >
            {createMutation.isPending ? "등록 중..." : "인증글 등록"}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default CertificationPostCreatePage;
