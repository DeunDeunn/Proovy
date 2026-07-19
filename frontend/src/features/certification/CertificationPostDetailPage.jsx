"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import {
  BadgeCheck,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Flag,
  Heart,
  ImageOff,
  MessageCircle,
  MoreVertical,
  Pencil,
  Trash2,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";

import CertificationPostComments from "./CertificationPostComments";
import { useCertificationPost, useDeleteCertificationPost } from "./hooks";
import ReportDialog from "./ReportDialog";

const formatCreatedAt = (value) => {
  if (!value) return "";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

const getAvatarInitial = (nickname) => Array.from(nickname?.trim() || "?")[0];

const ProfileAvatar = ({ nickname, profileImageUrl }) =>
  profileImageUrl ? (
    <img
      src={profileImageUrl}
      alt={`${nickname ?? "사용자"} 프로필 이미지`}
      className="h-10 w-10 shrink-0 rounded-full border border-gray-200 object-cover"
    />
  ) : (
    <span
      aria-hidden="true"
      className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-light text-sm font-bold text-primary"
    >
      {getAvatarInitial(nickname)}
    </span>
  );

const CertificationPostDetailPage = ({ postId }) => {
  const router = useRouter();
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [isPostMenuOpen, setIsPostMenuOpen] = useState(false);
  const [isReportDialogOpen, setIsReportDialogOpen] = useState(false);
  const { data: post, error, isLoading } = useCertificationPost(postId);
  const { data: me } = useMe();
  const deletePostMutation = useDeleteCertificationPost();

  if (isLoading) return <Loading label="인증 게시글을 불러오는 중..." />;
  if (error) return <ErrorMessage error={error} />;
  if (!post) return null;

  const images = [post.thumbnailUrl, ...(post.imageUrls ?? [])].filter(Boolean);
  const displayedImageIndex = Math.min(currentImageIndex, Math.max(images.length - 1, 0));
  const currentImageUrl = images[displayedImageIndex];
  const isAuthor = me?.id != null && me.id === post.authorId;

  const deletePost = () => {
    if (!window.confirm("인증 게시글을 삭제할까요?")) return;

    setIsPostMenuOpen(false);
    deletePostMutation.mutate(postId, {
      onSuccess: () => router.back(),
    });
  };

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">인증 게시글</h1>

      <Card className="overflow-hidden p-0 lg:flex lg:h-[680px]">
        <section className="relative flex aspect-square items-center justify-center bg-black lg:aspect-auto lg:w-3/5">
          {currentImageUrl ? (
            <>
              <img
                src={currentImageUrl}
                alt={`${displayedImageIndex + 1}번째 인증 이미지`}
                className="h-full w-full object-contain"
              />

              {images.length > 1 && (
                <>
                  <button
                    type="button"
                    onClick={() => setCurrentImageIndex((index) => Math.max(0, index - 1))}
                    disabled={displayedImageIndex === 0}
                    aria-label="이전 이미지"
                    className="absolute left-3 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-black/55 text-white transition-colors hover:bg-black/70 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <ChevronLeft size={22} />
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      setCurrentImageIndex((index) => Math.min(images.length - 1, index + 1))
                    }
                    disabled={displayedImageIndex === images.length - 1}
                    aria-label="다음 이미지"
                    className="absolute right-3 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-black/55 text-white transition-colors hover:bg-black/70 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <ChevronRight size={22} />
                  </button>
                  <span
                    aria-live="polite"
                    className="absolute right-3 top-3 rounded-full bg-black/55 px-2.5 py-1 text-xs font-medium text-white"
                  >
                    {displayedImageIndex + 1} / {images.length}
                  </span>
                </>
              )}
            </>
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-gray-100 text-gray-400">
              <ImageOff size={40} aria-hidden="true" />
              <span className="sr-only">대표 인증 이미지 없음</span>
            </div>
          )}
        </section>

        <aside className="flex min-h-[540px] flex-1 flex-col bg-surface lg:min-h-0">
          <div className="border-b border-gray-100 p-5">
            <div className="flex items-start justify-between gap-4">
              <div className="flex min-w-0 items-center gap-3">
                <ProfileAvatar
                  nickname={post.authorNickname}
                  profileImageUrl={post.authorProfileImageUrl}
                />
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <p className="truncate text-sm font-semibold text-gray-900">
                      {post.authorNickname ?? "알 수 없는 사용자"}
                    </p>
                    {post.authorBadgeApproved && (
                      <span className="inline-flex text-sky-500" title="인증 회원">
                        <BadgeCheck size={17} aria-hidden="true" />
                        <span className="sr-only">인증 회원</span>
                      </span>
                    )}
                  </div>
                  <div className="mt-1 flex items-center gap-1.5 text-xs text-gray-400">
                    <CalendarDays size={14} aria-hidden="true" />
                    <span>{formatCreatedAt(post.createdAt)}</span>
                  </div>
                </div>
              </div>
              {me?.id != null && (
                <div className="relative shrink-0">
                  <button
                    type="button"
                    onClick={() => setIsPostMenuOpen((open) => !open)}
                    aria-label="게시글 메뉴"
                    aria-expanded={isPostMenuOpen}
                    className="flex h-9 w-9 items-center justify-center rounded-full text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900"
                  >
                    <MoreVertical size={20} aria-hidden="true" />
                  </button>

                  {isPostMenuOpen && (
                    <div
                      role="menu"
                      className="absolute right-0 top-10 z-20 w-32 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-lg"
                    >
                      {isAuthor ? (
                        <>
                          <button
                            type="button"
                            role="menuitem"
                            onClick={() => router.push(`/certification-posts/${postId}/edit`)}
                            disabled={deletePostMutation.isPending}
                            className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:text-gray-400"
                          >
                            <Pencil size={15} aria-hidden="true" />
                            수정
                          </button>
                          <button
                            type="button"
                            role="menuitem"
                            onClick={deletePost}
                            disabled={deletePostMutation.isPending}
                            className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-danger hover:bg-red-50 disabled:cursor-not-allowed disabled:text-gray-400"
                          >
                            <Trash2 size={15} aria-hidden="true" />
                            {deletePostMutation.isPending ? "삭제 중..." : "삭제"}
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          role="menuitem"
                          onClick={() => {
                            setIsPostMenuOpen(false);
                            setIsReportDialogOpen(true);
                          }}
                          className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50"
                        >
                          <Flag size={15} aria-hidden="true" />
                          신고
                        </button>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            <p className="mt-5 whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">
              {post.contents || "작성한 인증 내용이 없습니다."}
            </p>

            {deletePostMutation.error && (
              <div className="mt-4">
                <ErrorMessage error={deletePostMutation.error} />
              </div>
            )}

            <div className="mt-5 flex items-center gap-5 text-sm text-gray-500">
              <span className="flex items-center gap-1.5">
                <Heart size={18} aria-hidden="true" />
                좋아요 {Number(post.likeCount ?? 0).toLocaleString()}
              </span>
              <span className="flex items-center gap-1.5">
                <MessageCircle size={18} aria-hidden="true" />
                댓글 {Number(post.commentCount ?? 0).toLocaleString()}
              </span>
            </div>
          </div>

          <CertificationPostComments
            postId={postId}
            status={post.status}
            commentCount={post.commentCount}
            embedded
          />
        </aside>
      </Card>

      <div className="mt-8 flex justify-end">
        <Button type="button" variant="outline" onClick={() => router.back()}>
          이전으로
        </Button>
      </div>

      {isReportDialogOpen && (
        <ReportDialog
          targetType="POST"
          targetId={post.postId}
          onClose={() => setIsReportDialogOpen(false)}
        />
      )}
    </div>
  );
};

export default CertificationPostDetailPage;
