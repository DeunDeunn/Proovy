"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import { BadgeCheck, CornerDownRight, MessageCircle } from "lucide-react";
import { useState } from "react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

import { useComments, useCreateComment } from "./hooks";

const formatCreatedAt = (value) => {
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

const CommentMeta = ({ comment }) => (
  <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-gray-400">
    <span>{formatCreatedAt(comment.createdAt)}</span>
    {comment.edited && <span>수정됨</span>}
  </div>
);

const getAvatarInitial = (nickname) => Array.from(nickname?.trim() || "?")[0];

const ProfileAvatar = ({ nickname, profileImageUrl, compact = false }) => {
  const sizeClassName = compact ? "h-8 w-8 text-xs" : "h-9 w-9 text-sm";
  const [failedImageUrl, setFailedImageUrl] = useState(null);
  const showProfileImage =
    Boolean(profileImageUrl) && failedImageUrl !== profileImageUrl;

  return showProfileImage ? (
    <img
      src={profileImageUrl}
      alt={`${nickname ?? "사용자"} 프로필 이미지`}
      onError={() => setFailedImageUrl(profileImageUrl)}
      className={`${sizeClassName} shrink-0 rounded-full border border-gray-200 object-cover`}
    />
  ) : (
    <span
      aria-hidden="true"
      className={`flex ${sizeClassName} shrink-0 items-center justify-center rounded-full bg-primary-light font-bold text-primary`}
    >
      {getAvatarInitial(nickname)}
    </span>
  );
};

const AuthorName = ({ nickname, badgeApproved }) => (
  <div className="flex min-w-0 items-center gap-1.5">
    <p className="truncate text-sm font-semibold text-gray-800">
      {nickname ?? "알 수 없는 사용자"}
    </p>
    {badgeApproved && (
      <span className="inline-flex shrink-0 text-sky-500" title="인증 회원">
        <BadgeCheck size={16} aria-hidden="true" />
        <span className="sr-only">인증 회원</span>
      </span>
    )}
  </div>
);

const CertificationPostComments = ({ postId, status, commentCount, embedded = false }) => {
  const [contents, setContents] = useState("");
  const [replyTargetId, setReplyTargetId] = useState(null);
  const [replyContents, setReplyContents] = useState("");
  const [formError, setFormError] = useState(null);
  const [errorParentCommentId, setErrorParentCommentId] = useState(null);
  const {
    data,
    error: commentsError,
    fetchNextPage,
    hasNextPage,
    isError: isCommentsError,
    isFetchingNextPage,
    isLoading,
  } = useComments(postId);
  const createMutation = useCreateComment(postId);

  const canWriteComment = status === "APPROVED";
  const comments = data?.pages.flat() ?? [];

  const clearFormError = () => {
    setFormError(null);
    setErrorParentCommentId(null);
    createMutation.reset();
  };

  const submitComment = (event, parentCommentId = null) => {
    event.preventDefault();
    const value = parentCommentId === null ? contents : replyContents;

    if (!value.trim()) {
      setFormError({ message: "댓글 내용을 입력해주세요." });
      setErrorParentCommentId(parentCommentId);
      return;
    }

    clearFormError();
    createMutation.mutate(
      {
        contents: value.trim(),
        parentCommentId,
      },
      {
        onSuccess: () => {
          if (parentCommentId === null) {
            setContents("");
          } else {
            setReplyContents("");
            setReplyTargetId(null);
          }
        },
        onError: (error) => {
          setFormError(error);
          setErrorParentCommentId(parentCommentId);
        },
      }
    );
  };

  const toggleReplyForm = (commentId) => {
    setReplyTargetId((current) => (current === commentId ? null : commentId));
    setReplyContents("");
    clearFormError();
  };

  const commentList = (
    <div
      className={
        embedded
          ? "min-h-0 flex-1 overflow-y-auto border-y border-gray-100 py-4 pr-1"
          : "mt-6 border-t border-gray-100 pt-5"
      }
    >
      {isLoading ? (
        <Loading label="댓글을 불러오는 중..." />
      ) : isCommentsError ? (
        <ErrorMessage error={commentsError} />
      ) : comments.length === 0 ? (
        <p className="py-6 text-center text-sm text-gray-500">첫 댓글을 남겨보세요.</p>
      ) : (
        <div className="space-y-5">
          {comments.map((comment) => (
            <article key={comment.commentId}>
              {comment.deleted ? (
                <p className="text-sm text-gray-400">삭제된 댓글입니다.</p>
              ) : (
                <div className="flex gap-3">
                  <ProfileAvatar
                    nickname={comment.authorNickname}
                    profileImageUrl={comment.authorProfileImageUrl}
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <AuthorName
                          nickname={comment.authorNickname}
                          badgeApproved={comment.authorBadgeApproved}
                        />
                        <div className="mt-1">
                          <CommentMeta comment={comment} />
                        </div>
                      </div>
                      {canWriteComment && (
                        <button
                          type="button"
                          onClick={() => toggleReplyForm(comment.commentId)}
                          className="shrink-0 text-xs font-medium text-primary hover:text-primary-hover"
                        >
                          답글
                        </button>
                      )}
                    </div>
                    <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">
                      {comment.contents}
                    </p>
                  </div>
                </div>
              )}

              {(comment.replies ?? []).length > 0 && (
                <div className="mt-4 space-y-4 border-l-2 border-gray-100 pl-4">
                  {comment.replies.map((reply) => (
                    <div key={reply.commentId} className="flex gap-2">
                      <CornerDownRight
                        size={16}
                        className="mt-2 shrink-0 text-gray-400"
                        aria-hidden="true"
                      />
                      <ProfileAvatar
                        nickname={reply.authorNickname}
                        profileImageUrl={reply.authorProfileImageUrl}
                        compact
                      />
                      <div className="min-w-0 flex-1">
                        <AuthorName
                          nickname={reply.authorNickname}
                          badgeApproved={reply.authorBadgeApproved}
                        />
                        <div className="mt-1">
                          <CommentMeta comment={reply} />
                        </div>
                        <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">
                          {reply.contents}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {canWriteComment && !comment.deleted && replyTargetId === comment.commentId && (
                <form
                  onSubmit={(event) => submitComment(event, comment.commentId)}
                  className="mt-4"
                >
                  <label htmlFor={`reply-${comment.commentId}`} className="sr-only">
                    답글 내용
                  </label>
                  <textarea
                    id={`reply-${comment.commentId}`}
                    value={replyContents}
                    onChange={(event) => setReplyContents(event.target.value)}
                    placeholder={`${comment.authorNickname}님에게 답글 남기기`}
                    rows={2}
                    disabled={createMutation.isPending}
                    className="w-full resize-y rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-gray-50"
                  />
                  <div className="mt-2 flex justify-end gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => toggleReplyForm(comment.commentId)}
                      disabled={createMutation.isPending}
                    >
                      취소
                    </Button>
                    <Button type="submit" disabled={createMutation.isPending}>
                      {createMutation.isPending ? "등록 중..." : "답글 등록"}
                    </Button>
                  </div>
                  {formError && errorParentCommentId === comment.commentId && (
                    <div className="mt-3">
                      <ErrorMessage error={formError} />
                    </div>
                  )}
                </form>
              )}
            </article>
          ))}

          {hasNextPage && (
            <div className="flex justify-center pt-1">
              <Button
                type="button"
                variant="outline"
                onClick={() => fetchNextPage()}
                disabled={isFetchingNextPage}
              >
                {isFetchingNextPage ? "불러오는 중..." : "댓글 더 보기"}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );

  const commentComposer = canWriteComment ? (
    <form onSubmit={(event) => submitComment(event)}>
      <label htmlFor="new-comment" className="sr-only">
        댓글 내용
      </label>
      <textarea
        id="new-comment"
        value={contents}
        onChange={(event) => setContents(event.target.value)}
        placeholder="따뜻한 응원 댓글을 남겨주세요."
        rows={embedded ? 2 : 3}
        disabled={createMutation.isPending}
        className={`w-full ${embedded ? "resize-none" : "resize-y"} rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-gray-50`}
      />
      <div className="mt-3 flex justify-end">
        <Button type="submit" disabled={createMutation.isPending}>
          {createMutation.isPending ? "등록 중..." : "댓글 등록"}
        </Button>
      </div>
    </form>
  ) : (
    <p className="rounded-lg bg-gray-50 px-4 py-3 text-sm text-gray-500">
      승인된 인증글에만 댓글을 작성할 수 있어요.
    </p>
  );

  const content = (
    <>
      <div className="flex items-center gap-2">
        <MessageCircle size={19} className="text-primary" aria-hidden="true" />
        <h2 id="comments-heading" className="font-semibold text-gray-900">
          댓글 {Number(commentCount ?? 0).toLocaleString()}
        </h2>
      </div>
      {commentList}
      <div className={embedded ? "pt-4" : "mt-5"}>
        {commentComposer}
        {formError && errorParentCommentId === null && (
          <div className="mt-3">
            <ErrorMessage error={formError} />
          </div>
        )}
      </div>
    </>
  );

  return (
    <section
      className={embedded ? "flex min-h-0 flex-1 flex-col p-5" : "mt-6"}
      aria-labelledby="comments-heading"
    >
      {embedded ? (
        <div className="flex min-h-0 flex-1 flex-col">{content}</div>
      ) : (
        <Card>{content}</Card>
      )}
    </section>
  );
};

export default CertificationPostComments;
