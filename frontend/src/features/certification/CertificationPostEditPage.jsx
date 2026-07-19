"use client";

/* eslint-disable @next/next/no-img-element -- S3 URL과 선택 직후의 blob: URL을 함께 미리보기로 사용한다. */

import { ImagePlus, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

import { useCertificationPost, useUpdateCertificationPost } from "./hooks";

const MAX_IMAGE_SIZE = 10 * 1024 * 1024;
const MAX_ADDITIONAL_IMAGES = 3;
const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const IMAGE_DOWNLOAD_TIMEOUT_MS = 15_000;

const getFileError = (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return "JPEG, PNG, WebP 이미지 파일만 첨부할 수 있습니다.";
  }

  if (file.size > MAX_IMAGE_SIZE) {
    return "이미지 파일은 한 장당 10MB 이하만 첨부할 수 있습니다.";
  }

  return null;
};

const getFileNameFromUrl = (url, fallbackName) => {
  try {
    const fileName = new URL(url).pathname.split("/").pop();
    return fileName || fallbackName;
  } catch {
    return fallbackName;
  }
};

const downloadExistingImageAsFile = async (url, fallbackName) => {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), IMAGE_DOWNLOAD_TIMEOUT_MS);

  try {
    const response = await fetch(url, { signal: controller.signal });
    if (!response.ok) {
      throw new Error("기존 이미지를 다시 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
    }

    const blob = await response.blob();
    if (blob.size === 0) {
      throw new Error("기존 이미지 파일이 비어 있습니다. 새 이미지를 선택해주세요.");
    }

    return new File([blob], getFileNameFromUrl(url, fallbackName), {
      type: blob.type || "application/octet-stream",
    });
  } catch (error) {
    if (error?.name === "AbortError") {
      throw new Error(
        "기존 이미지를 불러오는 시간이 초과되었습니다. 네트워크를 확인한 후 다시 시도해주세요."
      );
    }

    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
};

const CertificationPostEditForm = ({ post, postId }) => {
  const router = useRouter();
  const updateMutation = useUpdateCertificationPost(postId);
  const [contents, setContents] = useState(post.contents ?? "");
  const [thumbnail, setThumbnail] = useState({ type: "existing", url: post.thumbnailUrl });
  const [additionalImages, setAdditionalImages] = useState(() =>
    (post.imageUrls ?? []).map((url) => ({ type: "existing", url }))
  );
  const [validationError, setValidationError] = useState(null);
  const [isPreparingFiles, setIsPreparingFiles] = useState(false);
  const thumbnailPreviewRef = useRef(null);
  const additionalPreviewsRef = useRef(new Set());
  const isSubmitting = isPreparingFiles || updateMutation.isPending;

  useEffect(
    () => () => {
      if (thumbnailPreviewRef.current) {
        URL.revokeObjectURL(thumbnailPreviewRef.current);
      }
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

    if (thumbnailPreviewRef.current) {
      URL.revokeObjectURL(thumbnailPreviewRef.current);
    }

    const preview = URL.createObjectURL(file);
    thumbnailPreviewRef.current = preview;
    setThumbnail({ type: "new", file, preview });
    setValidationError(null);
    event.target.value = "";
  };

  const restoreExistingThumbnail = () => {
    if (thumbnailPreviewRef.current) {
      URL.revokeObjectURL(thumbnailPreviewRef.current);
      thumbnailPreviewRef.current = null;
    }
    setThumbnail({ type: "existing", url: post.thumbnailUrl });
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
      type: "new",
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
      if (target?.type === "new") {
        URL.revokeObjectURL(target.preview);
        additionalPreviewsRef.current.delete(target.preview);
      }
      return currentImages.filter((_, imageIndex) => imageIndex !== index);
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setValidationError(null);
    setIsPreparingFiles(true);

    try {
      // 현재 수정 API는 파일 전체 교체 방식이므로, 남겨둔 기존 URL도 File로 다시 만들어 보낸다.
      const thumbnailFile =
        thumbnail.type === "new"
          ? thumbnail.file
          : await downloadExistingImageAsFile(thumbnail.url, "existing-thumbnail.jpg");
      const imageFiles = await Promise.all(
        additionalImages.map((image, index) =>
          image.type === "new"
            ? image.file
            : downloadExistingImageAsFile(image.url, `existing-image-${index + 1}.jpg`)
        )
      );

      await updateMutation.mutateAsync({
        contents: contents.trim(),
        thumbnail: thumbnailFile,
        images: imageFiles,
      });
      router.replace(`/certification-posts/${postId}`);
    } catch (error) {
      const isImageDownloadFailure = error instanceof TypeError;
      setValidationError(
        isImageDownloadFailure
          ? "기존 이미지를 다시 불러오지 못했습니다. S3 CORS 설정 또는 네트워크 연결을 확인해주세요."
          : (error?.message ?? "인증글을 수정하지 못했습니다. 잠시 후 다시 시도해주세요.")
      );
    } finally {
      setIsPreparingFiles(false);
    }
  };

  const thumbnailUrl = thumbnail.type === "new" ? thumbnail.preview : thumbnail.url;

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">인증 게시글 수정</h1>
        <p className="mt-2 text-sm text-gray-500">
          남겨둔 기존 이미지는 다시 업로드해 유지하고, 제거한 추가 이미지는 삭제됩니다.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <div className="mb-4 flex items-baseline justify-between gap-4">
            <div>
              <h2 className="font-semibold text-gray-900">대표 인증 이미지</h2>
              <p className="mt-1 text-sm text-gray-500">
                새 이미지를 선택하지 않으면 현재 대표 이미지를 그대로 사용합니다.
              </p>
            </div>
            <span className="shrink-0 text-xs font-medium text-danger">필수</span>
          </div>

          <div className="relative aspect-video overflow-hidden rounded-lg bg-gray-100">
            <img
              src={thumbnailUrl}
              alt="대표 인증 이미지 미리보기"
              className="h-full w-full object-cover"
            />
            {thumbnail.type === "new" && (
              <button
                type="button"
                onClick={restoreExistingThumbnail}
                aria-label="새 대표 이미지 선택 취소"
                className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/75"
              >
                <X size={18} />
              </button>
            )}
          </div>

          <label className="mt-3 inline-flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:border-primary hover:text-primary">
            <ImagePlus size={18} />
            대표 이미지 변경
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={handleThumbnailChange}
              className="sr-only"
              disabled={isSubmitting}
            />
          </label>
        </Card>

        <Card>
          <div className="mb-4">
            <h2 className="font-semibold text-gray-900">추가 이미지</h2>
            <p className="mt-1 text-sm text-gray-500">
              기존 이미지를 삭제하거나 새 이미지를 더해 최대 3장까지 구성할 수 있어요.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {additionalImages.map((image, index) => {
              const imageUrl = image.type === "new" ? image.preview : image.url;
              const imageKey = image.type === "new" ? image.preview : `${image.url}-${index}`;

              return (
                <div
                  key={imageKey}
                  className="relative aspect-square overflow-hidden rounded-lg bg-gray-100"
                >
                  <img
                    src={imageUrl}
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
              );
            })}

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
                  disabled={isSubmitting}
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
            rows={6}
            className="mt-4 w-full resize-y rounded-lg border border-gray-200 px-4 py-3 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
            disabled={isSubmitting}
          />
        </Card>

        {(validationError || updateMutation.isError) && (
          <ErrorMessage
            error={validationError ? { message: validationError } : updateMutation.error}
          />
        )}

        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.back()}
            disabled={isSubmitting}
          >
            취소
          </Button>
          <Button type="submit" className="min-w-28" disabled={isSubmitting}>
            {isSubmitting ? "수정 중..." : "인증글 수정"}
          </Button>
        </div>
      </form>
    </div>
  );
};

const CertificationPostEditPage = ({ postId }) => {
  const { data: post, error, isLoading } = useCertificationPost(postId);

  if (isLoading) return <Loading label="인증 게시글을 불러오는 중..." />;
  if (error) return <ErrorMessage error={error} />;
  if (!post) return null;

  return <CertificationPostEditForm key={postId} post={post} postId={postId} />;
};

export default CertificationPostEditPage;
