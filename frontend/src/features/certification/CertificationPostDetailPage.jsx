"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import {
  BadgeCheck,
  Bookmark,
  ChevronLeft,
  ChevronRight,
  Flag,
  Heart,
  ImageOff,
  MessageCircle,
  MoreVertical,
  Pencil,
  Send,
  Trash2,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useRef, useState } from "react";

import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";

import CertificationPostComments from "./CertificationPostComments";
import DeleteCertificationPostDialog from "./DeleteCertificationPostDialog";
import {
  useCertificationPost,
  useDeleteCertificationPost,
  useToggleCertificationPostLike,
} from "./hooks";
import ReportDialog from "./ReportDialog";
import { useDismissable } from "./useDismissable";

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

const PostReactionBar = ({
  liked,
  likeCount,
  commentCount,
  createdAt,
  canLike,
  isLikePending,
  likeError,
  onToggleLike,
}) => (
  <div className="border-t border-gray-100 px-5 py-4">
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-4 text-gray-900">
        <button
          type="button"
          onClick={onToggleLike}
          disabled={!canLike || isLikePending}
          title={canLike ? "좋아요" : "승인된 인증글에만 좋아요할 수 있어요."}
          aria-label={liked ? "좋아요 취소" : "좋아요"}
          aria-pressed={liked}
          className={`inline-flex items-center gap-1.5 transition-transform hover:scale-110 disabled:cursor-not-allowed disabled:opacity-40 ${liked ? "text-rose-500" : "text-gray-900"}`}
        >
          <Heart
            size={25}
            strokeWidth={1.8}
            fill={liked ? "currentColor" : "none"}
            aria-hidden="true"
          />
          <span className="text-sm font-semibold">
            {Number(likeCount ?? 0).toLocaleString()}
          </span>
          <span className="sr-only">좋아요</span>
        </button>
        <span className="inline-flex items-center gap-1.5" title="댓글">
          <MessageCircle size={24} strokeWidth={1.8} aria-hidden="true" />
          <span className="text-sm font-semibold">
            {Number(commentCount ?? 0).toLocaleString()}
          </span>
          <span className="sr-only">댓글</span>
        </span>
        <span className="inline-flex" title="공유">
          <Send size={24} strokeWidth={1.8} aria-hidden="true" />
          <span className="sr-only">공유</span>
        </span>
      </div>
      <span className="inline-flex" title="저장">
        <Bookmark size={24} strokeWidth={1.8} aria-hidden="true" />
        <span className="sr-only">저장</span>
      </span>
    </div>
    <p className="mt-3 text-xs text-gray-400">{formatCreatedAt(createdAt)}</p>
    {likeError && (
      <div className="mt-3">
        <ErrorMessage error={likeError} />
      </div>
    )}
  </div>
);

const CertificationPostDetailPage = ({ postId }) => {
  const router = useRouter();
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [isPostMenuOpen, setIsPostMenuOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isReportDialogOpen, setIsReportDialogOpen] = useState(false);
  const postMenuRef = useRef(null);
  const { data: post, error, isLoading } = useCertificationPost(postId);
  const { data: me } = useMe();
  const deletePostMutation = useDeleteCertificationPost();
  const toggleLikeMutation = useToggleCertificationPostLike(postId);
  const closePostMenu = useCallback(() => setIsPostMenuOpen(false), []);

  useDismissable(isPostMenuOpen, postMenuRef, closePostMenu);

  if (isLoading) return <Loading label="인증 게시글을 불러오는 중..." />;
  if (error) return <ErrorMessage error={error} />;
  if (!post) return null;

  const images = [post.thumbnailUrl, ...(post.imageUrls ?? [])].filter(Boolean);
  const displayedImageIndex = Math.min(currentImageIndex, Math.max(images.length - 1, 0));
  const currentImageUrl = images[displayedImageIndex];
  const isAuthor = me?.id != null && me.id === post.authorId;

  const deletePost = () => {
    deletePostMutation.mutate(postId, {
      onSuccess: () => router.back(),
    });
  };

  return (
    <div className="mx-auto max-w-[1440px]">
      <Card className="overflow-hidden rounded-2xl p-0 lg:flex lg:h-[min(720px,calc(100vh-6rem))]">
        <section className="relative flex aspect-square items-center justify-center bg-black lg:aspect-auto lg:w-[58%]">
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

        <aside className="flex min-h-[560px] flex-1 flex-col bg-surface lg:min-h-0 lg:w-[42%]">
          <header className="shrink-0 border-b border-gray-100 px-5 py-4">
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
                </div>
              </div>
              {me?.id != null && (
                <div ref={postMenuRef} className="relative shrink-0">
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
                            onClick={() => {
                              setIsPostMenuOpen(false);
                              setIsDeleteDialogOpen(true);
                            }}
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
          </header>

          <div className="max-h-48 shrink-0 overflow-y-auto border-b border-gray-100 px-5 py-3">
            <p className="whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">
              {post.contents || "작성한 인증 내용이 없습니다."}
            </p>
          </div>

          {deletePostMutation.error && (
            <div className="shrink-0 border-b border-gray-100 px-5 py-4">
              <ErrorMessage error={deletePostMutation.error} />
            </div>
          )}

          <CertificationPostComments
            postId={postId}
            status={post.status}
            commentCount={post.commentCount}
            embedded
            footer={
              <PostReactionBar
                likeCount={post.likeCount}
                commentCount={post.commentCount}
                createdAt={post.createdAt}
                liked={post.liked}
                canLike={post.status === "APPROVED"}
                isLikePending={toggleLikeMutation.isPending}
                likeError={toggleLikeMutation.error}
                onToggleLike={() => toggleLikeMutation.mutate()}
              />
            }
          />
        </aside>
      </Card>

      {isReportDialogOpen && (
        <ReportDialog
          targetType="POST"
          targetId={post.postId}
          onClose={() => setIsReportDialogOpen(false)}
        />
      )}
      {isDeleteDialogOpen && (
        <DeleteCertificationPostDialog
          isDeleting={deletePostMutation.isPending}
          onClose={() => setIsDeleteDialogOpen(false)}
          onDelete={deletePost}
        />
      )}
    </div>
  );
};

export default CertificationPostDetailPage;
