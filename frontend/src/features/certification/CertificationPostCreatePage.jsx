"use client";

/* eslint-disable @next/next/no-img-element -- 선택 직후의 blob: URL 미리보기에는 next/image 최적화를 적용할 수 없다. */

import { ImagePlus, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useChallenge } from "@/features/challenge/hooks";

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
        onSuccess: () => router.push("/mypage/feed"),
      }
    );
  };

  if (isChallengeLoading) return <Loading label="챌린지 정보를 불러오는 중..." />;
  if (challengeError) return <ErrorMessage error={challengeError} />;

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6">
        <p className="text-sm font-medium text-primary">
          {challenge?.title ?? `챌린지 #${challengeId}`}
        </p>
        <h1 className="mt-1 text-2xl font-bold text-gray-900">인증 게시글 작성</h1>
        <p className="mt-2 text-sm text-gray-500">
          대표 인증 사진을 첨부하고 오늘의 챌린지를 기록해보세요.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <div className="mb-4 flex items-baseline justify-between gap-4">
            <div>
              <h2 className="font-semibold text-gray-900">대표 인증 이미지</h2>
              <p className="mt-1 text-sm text-gray-500">
                인증에 사용할 사진 한 장을 반드시 첨부해주세요.
              </p>
            </div>
            <span className="shrink-0 text-xs font-medium text-danger">필수</span>
          </div>

          {thumbnailPreview ? (
            <div className="relative aspect-video overflow-hidden rounded-lg bg-gray-100">
              <img
                src={thumbnailPreview}
                alt="대표 인증 이미지 미리보기"
                className="h-full w-full object-cover"
              />
              <button
                type="button"
                onClick={() => {
                  setThumbnail(null);
                  setThumbnailPreview(null);
                }}
                aria-label="대표 이미지 제거"
                className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/75"
              >
                <X size={18} />
              </button>
            </div>
          ) : (
            <label className="flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-200 bg-gray-50 px-6 py-12 text-center transition-colors hover:border-primary hover:bg-primary-light">
              <ImagePlus size={30} className="mb-3 text-gray-400" />
              <span className="text-sm font-medium text-gray-700">대표 이미지 선택</span>
              <span className="mt-1 text-xs text-gray-400">JPEG, PNG, WebP · 최대 10MB</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleThumbnailChange}
                className="sr-only"
                disabled={createMutation.isPending}
              />
            </label>
          )}
        </Card>

        <Card>
          <div className="mb-4">
            <h2 className="font-semibold text-gray-900">추가 이미지</h2>
            <p className="mt-1 text-sm text-gray-500">
              인증을 더 잘 보여줄 사진을 최대 3장까지 첨부할 수 있어요.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {additionalImages.map((image, index) => (
              <div
                key={image.preview}
                className="relative aspect-square overflow-hidden rounded-lg bg-gray-100"
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
                  className="absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/75"
                >
                  <X size={16} />
                </button>
              </div>
            ))}

            {additionalImages.length < MAX_ADDITIONAL_IMAGES && (
              <label className="flex aspect-square cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-200 bg-gray-50 text-center transition-colors hover:border-primary hover:bg-primary-light">
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
          <p className="mt-3 text-xs text-gray-400">
            {additionalImages.length} / {MAX_ADDITIONAL_IMAGES}장 첨부됨
          </p>
        </Card>

        <Card>
          <label htmlFor="certification-contents" className="block font-semibold text-gray-900">
            인증 내용
          </label>
          <p className="mt-1 text-sm text-gray-500">오늘의 인증 과정을 자유롭게 남겨주세요.</p>
          <textarea
            id="certification-contents"
            value={contents}
            onChange={(event) => setContents(event.target.value)}
            placeholder="예: 오늘도 아침 6시에 일어나 30분 동안 달렸어요!"
            rows={6}
            className="mt-4 w-full resize-y rounded-lg border border-gray-200 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
            disabled={createMutation.isPending}
          />
        </Card>

        {(validationError || createMutation.isError) && (
          <ErrorMessage
            error={validationError ? { message: validationError } : createMutation.error}
          />
        )}

        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.back()}
            disabled={createMutation.isPending}
          >
            취소
          </Button>
          <Button type="submit" className="min-w-28" disabled={createMutation.isPending}>
            {createMutation.isPending ? "등록 중..." : "인증글 등록"}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default CertificationPostCreatePage;
