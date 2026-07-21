"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import {
  BadgeCheck,
  CornerDownRight,
  Flag,
  Heart,
  MessageCircle,
  MessageSquare,
  MoreVertical,
  Pencil,
  Trash2,
} from "lucide-react";
import { useCallback, useRef, useState } from "react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useMe } from "@/features/auth/hooks";
import { useStartDirectChat } from "@/features/chat/hooks/chatHooks";

import {
  useComments,
  useCreateComment,
  useDeleteComment,
  useToggleCommentLike,
  useUpdateComment,
} from "./hooks";
import ReportDialog from "./ReportDialog";
import { useDismissable } from "./useDismissable";

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
  <div className="flex flex-wrap items-center gap-x-1.5 text-[11px] text-gray-400">
    <span>{formatCreatedAt(comment.createdAt)}</span>
    {comment.edited && <span>수정됨</span>}
  </div>
);

const getAvatarInitial = (nickname) => Array.from(nickname?.trim() || "?")[0];

const ProfileAvatar = ({ nickname, profileImageUrl, compact = false }) => {
  const sizeClassName = compact ? "h-7 w-7 text-[11px]" : "h-8 w-8 text-xs";
  const [failedImageUrl, setFailedImageUrl] = useState(null);
  const showProfileImage = Boolean(profileImageUrl) && failedImageUrl !== profileImageUrl;

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
  <div className="flex min-w-0 items-center gap-1">
    <p className="truncate text-[13px] font-semibold text-gray-800">
      {nickname ?? "알 수 없는 사용자"}
    </p>
    {badgeApproved && (
      <span className="inline-flex shrink-0 text-sky-500" title="인증 회원">
        <BadgeCheck size={15} aria-hidden="true" />
        <span className="sr-only">인증 회원</span>
      </span>
    )}
  </div>
);

const CommentLikeButton = ({ comment, canLike, isPending, onToggle }) => (
  <button
    type="button"
    onClick={onToggle}
    disabled={!canLike || isPending}
    title={canLike ? "댓글 좋아요" : "승인된 인증글의 댓글에만 좋아요할 수 있어요."}
    aria-label={comment.liked ? "댓글 좋아요 취소" : "댓글 좋아요"}
    aria-pressed={comment.liked}
    className={`inline-flex items-center gap-0.5 rounded-full px-1 py-1 text-[11px] transition-colors hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-40 ${comment.liked ? "text-rose-500" : "text-gray-500"}`}
  >
    <Heart
      size={16}
      strokeWidth={1.8}
      fill={comment.liked ? "currentColor" : "none"}
      aria-hidden="true"
    />
    {Number(comment.likeCount ?? 0) > 0 && (
      <span className="font-medium">{Number(comment.likeCount).toLocaleString()}</span>
    )}
  </button>
);

const CommentKebabMenu = ({
  comment,
  currentUserId,
  isActionPending,
  isStartingChat,
  onEdit,
  onDelete,
  onReport,
  onStartChat,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);
  const isAuthor = currentUserId === comment.authorId;
  const closeMenu = useCallback(() => setIsOpen(false), []);

  useDismissable(isOpen, menuRef, closeMenu);

  if (currentUserId == null || comment.deleted) return null;

  const runAction = (action) => {
    setIsOpen(false);
    action();
  };

  return (
    <div ref={menuRef} className="relative shrink-0">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-label="댓글 메뉴"
        aria-expanded={isOpen}
        disabled={isActionPending}
        className="flex h-7 w-7 items-center justify-center rounded-full text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900 disabled:cursor-not-allowed disabled:text-gray-300"
      >
        <MoreVertical size={16} aria-hidden="true" />
      </button>

      {isOpen && (
        <div
          role="menu"
          className="absolute right-0 top-8 z-20 w-28 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-lg"
        >
          {isAuthor ? (
            <>
              <button
                type="button"
                role="menuitem"
                onClick={() => runAction(() => onEdit(comment))}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50"
              >
                <Pencil size={15} aria-hidden="true" />
                수정
              </button>
              <button
                type="button"
                role="menuitem"
                onClick={() => runAction(() => onDelete(comment.commentId))}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-danger hover:bg-red-50"
              >
                <Trash2 size={15} aria-hidden="true" />
                삭제
              </button>
            </>
          ) : (
            <>
              <button
                type="button"
                role="menuitem"
                onClick={() => runAction(() => onStartChat(comment.authorId))}
                disabled={isStartingChat}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:text-gray-400"
              >
                <MessageSquare size={15} aria-hidden="true" />
                채팅하기
              </button>
              <button
                type="button"
                role="menuitem"
                onClick={() => runAction(() => onReport(comment.commentId))}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50"
              >
                <Flag size={15} aria-hidden="true" />
                신고
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
};

const CertificationPostComments = ({ postId, status, commentCount, embedded = false, footer }) => {
  const [contents, setContents] = useState("");
  const [replyTargetId, setReplyTargetId] = useState(null);
  const [replyContents, setReplyContents] = useState("");
  const [formError, setFormError] = useState(null);
  const [errorParentCommentId, setErrorParentCommentId] = useState(null);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingContents, setEditingContents] = useState("");
  const [commentActionError, setCommentActionError] = useState(null);
  const [reportTargetId, setReportTargetId] = useState(null);
  const {
    data,
    error: commentsError,
    fetchNextPage,
    hasNextPage,
    isError: isCommentsError,
    isFetchingNextPage,
    isLoading,
  } = useComments(postId);
  const { data: me } = useMe();
  const { startChat, isPending: isStartingChat } = useStartDirectChat();
  const createMutation = useCreateComment(postId);
  const updateCommentMutation = useUpdateComment(postId);
  const deleteCommentMutation = useDeleteComment(postId);
  const toggleCommentLikeMutation = useToggleCommentLike(postId);

  const canWriteComment = status === "APPROVED";
  const comments = data?.pages.flat() ?? [];
  const isCommentActionPending = updateCommentMutation.isPending || deleteCommentMutation.isPending;
  const isCommentLikePending = (commentId) =>
    toggleCommentLikeMutation.isPending && toggleCommentLikeMutation.variables === commentId;

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
    if (createMutation.isPending) return;

    setReplyTargetId((current) => (current === commentId ? null : commentId));
    setReplyContents("");
    clearFormError();
  };

  const startEditingComment = (comment) => {
    if (isCommentActionPending) return;

    setCommentActionError(null);
    setEditingCommentId(comment.commentId);
    setEditingContents(comment.contents ?? "");
  };

  const cancelEditingComment = () => {
    if (updateCommentMutation.isPending) return;

    setCommentActionError(null);
    setEditingCommentId(null);
    setEditingContents("");
  };

  const submitCommentUpdate = (event, commentId) => {
    event.preventDefault();
    const contents = editingContents.trim();

    if (!contents) {
      setCommentActionError({
        commentId,
        error: { message: "댓글 내용을 입력해주세요." },
      });
      return;
    }

    setCommentActionError(null);
    updateCommentMutation.mutate(
      { commentId, payload: { contents } },
      {
        onSuccess: () => {
          setEditingCommentId(null);
          setEditingContents("");
        },
        onError: (error) => setCommentActionError({ commentId, error }),
      }
    );
  };

  const deleteComment = (commentId) => {
    if (!window.confirm("댓글을 삭제할까요?")) return;

    setCommentActionError(null);
    deleteCommentMutation.mutate(commentId, {
      onSuccess: () => {
        if (editingCommentId === commentId) {
          setEditingCommentId(null);
          setEditingContents("");
        }
      },
      onError: (error) => setCommentActionError({ commentId, error }),
    });
  };

  const commentList = (
    <div
      className={
        embedded ? "min-h-0 flex-1 overflow-y-auto px-5 py-4" : "mt-6 border-t border-gray-100 pt-5"
      }
    >
      {isLoading ? (
        <Loading label="댓글을 불러오는 중..." />
      ) : isCommentsError ? (
        <ErrorMessage error={commentsError} />
      ) : comments.length === 0 ? (
        <p className="py-6 text-center text-sm text-gray-500">첫 댓글을 남겨보세요.</p>
      ) : (
        <div className="space-y-4">
          {comments.map((comment) => (
            <article key={comment.commentId}>
              {comment.deleted ? (
                <p className="text-sm text-gray-400">삭제된 댓글입니다.</p>
              ) : (
                <div className="flex items-start gap-2.5">
                  <div className="-mt-1.5">
                    <ProfileAvatar
                      nickname={comment.authorNickname}
                      profileImageUrl={comment.authorProfileImageUrl}
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex min-w-0 flex-wrap items-center gap-x-1.5 gap-y-0.5">
                        <AuthorName
                          nickname={comment.authorNickname}
                          badgeApproved={comment.authorBadgeApproved}
                        />
                        <CommentMeta comment={comment} />
                      </div>
                      <div className="flex shrink-0 items-center gap-0.5">
                        <CommentLikeButton
                          comment={comment}
                          canLike={canWriteComment}
                          isPending={isCommentLikePending(comment.commentId)}
                          onToggle={() => toggleCommentLikeMutation.mutate(comment.commentId)}
                        />
                        {canWriteComment && (
                          <button
                            type="button"
                            onClick={() => toggleReplyForm(comment.commentId)}
                            disabled={createMutation.isPending}
                            aria-label="답글 작성"
                            title="답글 작성"
                            className="inline-flex h-6 w-6 shrink-0 items-center justify-center text-gray-400 hover:text-primary disabled:cursor-not-allowed disabled:text-gray-400"
                          >
                            <CornerDownRight size={16} strokeWidth={1.8} aria-hidden="true" />
                          </button>
                        )}
                        <CommentKebabMenu
                          comment={comment}
                          currentUserId={me?.id}
                          isActionPending={isCommentActionPending}
                          isStartingChat={isStartingChat}
                          onEdit={startEditingComment}
                          onDelete={deleteComment}
                          onReport={setReportTargetId}
                          onStartChat={startChat}
                        />
                      </div>
                    </div>
                    {editingCommentId === comment.commentId ? (
                      <form
                        onSubmit={(event) => submitCommentUpdate(event, comment.commentId)}
                        className="mt-3"
                      >
                        <label htmlFor={`edit-comment-${comment.commentId}`} className="sr-only">
                          댓글 내용
                        </label>
                        <textarea
                          id={`edit-comment-${comment.commentId}`}
                          value={editingContents}
                          onChange={(event) => setEditingContents(event.target.value)}
                          rows={3}
                          disabled={updateCommentMutation.isPending}
                          className="w-full resize-y rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-gray-50"
                        />
                        <div className="mt-2 flex justify-end gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            onClick={cancelEditingComment}
                            disabled={updateCommentMutation.isPending}
                          >
                            취소
                          </Button>
                          <Button type="submit" disabled={updateCommentMutation.isPending}>
                            {updateCommentMutation.isPending ? "저장 중..." : "저장"}
                          </Button>
                        </div>
                      </form>
                    ) : (
                      <p className="mt-0.5 whitespace-pre-wrap break-words text-[13px] leading-5 text-gray-700">
                        {comment.contents}
                      </p>
                    )}
                    {commentActionError?.commentId === comment.commentId && (
                      <div className="mt-3">
                        <ErrorMessage error={commentActionError.error} />
                      </div>
                    )}
                    {toggleCommentLikeMutation.error &&
                      toggleCommentLikeMutation.variables === comment.commentId && (
                        <div className="mt-3">
                          <ErrorMessage error={toggleCommentLikeMutation.error} />
                        </div>
                      )}
                  </div>
                </div>
              )}

              {(comment.replies ?? []).length > 0 && (
                <div className="mt-3 space-y-3 border-l-2 border-gray-100 pl-3">
                  {comment.replies.map((reply) => (
                    <div key={reply.commentId} className="flex items-start gap-2">
                      <CornerDownRight
                        size={14}
                        className="mt-0.5 shrink-0 text-gray-400"
                        aria-hidden="true"
                      />
                      <div className="-mt-1">
                        <ProfileAvatar
                          nickname={reply.authorNickname}
                          profileImageUrl={reply.authorProfileImageUrl}
                          compact
                        />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex min-w-0 flex-wrap items-center gap-x-1.5 gap-y-0.5">
                            <AuthorName
                              nickname={reply.authorNickname}
                              badgeApproved={reply.authorBadgeApproved}
                            />
                            <CommentMeta comment={reply} />
                          </div>
                          <div className="flex shrink-0 items-center gap-0.5">
                            <CommentLikeButton
                              comment={reply}
                              canLike={canWriteComment}
                              isPending={isCommentLikePending(reply.commentId)}
                              onToggle={() => toggleCommentLikeMutation.mutate(reply.commentId)}
                            />
                            <CommentKebabMenu
                              comment={reply}
                              currentUserId={me?.id}
                              isActionPending={isCommentActionPending}
                              isStartingChat={isStartingChat}
                              onEdit={startEditingComment}
                              onDelete={deleteComment}
                              onReport={setReportTargetId}
                              onStartChat={startChat}
                            />
                          </div>
                        </div>
                        {editingCommentId === reply.commentId ? (
                          <form
                            onSubmit={(event) => submitCommentUpdate(event, reply.commentId)}
                            className="mt-3"
                          >
                            <label htmlFor={`edit-comment-${reply.commentId}`} className="sr-only">
                              댓글 내용
                            </label>
                            <textarea
                              id={`edit-comment-${reply.commentId}`}
                              value={editingContents}
                              onChange={(event) => setEditingContents(event.target.value)}
                              rows={3}
                              disabled={updateCommentMutation.isPending}
                              className="w-full resize-y rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-gray-50"
                            />
                            <div className="mt-2 flex justify-end gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                onClick={cancelEditingComment}
                                disabled={updateCommentMutation.isPending}
                              >
                                취소
                              </Button>
                              <Button type="submit" disabled={updateCommentMutation.isPending}>
                                {updateCommentMutation.isPending ? "저장 중..." : "저장"}
                              </Button>
                            </div>
                          </form>
                        ) : (
                          <p className="mt-0.5 whitespace-pre-wrap break-words text-[13px] leading-5 text-gray-700">
                            {reply.contents}
                          </p>
                        )}
                        {commentActionError?.commentId === reply.commentId && (
                          <div className="mt-3">
                            <ErrorMessage error={commentActionError.error} />
                          </div>
                        )}
                        {toggleCommentLikeMutation.error &&
                          toggleCommentLikeMutation.variables === reply.commentId && (
                            <div className="mt-3">
                              <ErrorMessage error={toggleCommentLikeMutation.error} />
                            </div>
                          )}
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
    <form
      onSubmit={(event) => submitComment(event)}
      className={embedded ? "flex items-center gap-3" : ""}
    >
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
        className={`w-full ${embedded ? "min-h-10 flex-1 resize-none border-0 px-0 py-2 focus:ring-0" : "resize-y rounded-lg border border-gray-200 px-3 py-2.5 focus:border-primary focus:ring-2 focus:ring-primary/20"} text-sm text-gray-900 outline-none placeholder:text-gray-400 disabled:bg-gray-50`}
      />
      {embedded ? (
        <button
          type="submit"
          disabled={createMutation.isPending}
          className="shrink-0 text-sm font-semibold text-primary transition-colors hover:text-primary-hover disabled:cursor-not-allowed disabled:text-gray-400"
        >
          {createMutation.isPending ? "등록 중" : "게시"}
        </button>
      ) : (
        <div className="mt-3 flex justify-end">
          <Button type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? "등록 중..." : "댓글 등록"}
          </Button>
        </div>
      )}
    </form>
  ) : (
    <p className="rounded-lg bg-gray-50 px-4 py-3 text-sm text-gray-500">
      승인된 인증글에만 댓글을 작성할 수 있어요.
    </p>
  );

  const content = (
    <>
      {embedded ? (
        <h2 id="comments-heading" className="sr-only">
          댓글 {Number(commentCount ?? 0).toLocaleString()}
        </h2>
      ) : (
        <div className="flex items-center gap-2">
          <MessageCircle size={19} className="text-primary" aria-hidden="true" />
          <h2 id="comments-heading" className="font-semibold text-gray-900">
            댓글 {Number(commentCount ?? 0).toLocaleString()}
          </h2>
        </div>
      )}
      {commentList}
      {embedded && footer}
      <div className={embedded ? "border-t border-gray-100 px-5 py-3" : "mt-5"}>
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
      className={embedded ? "flex min-h-0 flex-1 flex-col" : "mt-6"}
      aria-labelledby="comments-heading"
    >
      {embedded ? (
        <div className="flex min-h-0 flex-1 flex-col">{content}</div>
      ) : (
        <Card>{content}</Card>
      )}
      {reportTargetId != null && (
        <ReportDialog
          targetType="COMMENT"
          targetId={reportTargetId}
          onClose={() => setReportTargetId(null)}
        />
      )}
    </section>
  );
};

export default CertificationPostComments;
