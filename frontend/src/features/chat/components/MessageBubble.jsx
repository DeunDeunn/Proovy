import { BadgeCheck, FileText, Image as ImageIcon, Trash2 } from "lucide-react";

import { formatChatTime, formatFileSize, getAvatarColor } from "@/features/chat/mockData";

const MessageBubble = ({ message, isOwn, showSenderInfo, isChallenge, onDelete, isDeletePending }) => {
  const avatarColor = getAvatarColor(message.senderId);
  const time = formatChatTime(message.createdAt);
  const readLabel = isOwn && !isChallenge && message.read ? "읽음" : null;
  const canDelete = isOwn && !message.deletedAt && !!onDelete;

  const handleDelete = () => {
    if (!window.confirm("메시지를 삭제할까요?")) return;
    onDelete(message.messageId);
  };

  const renderContent = () => {
    if (message.deletedAt) {
      return (
        <div className="rounded-2xl bg-gray-100 px-4 py-2.5 text-sm italic text-gray-400">
          삭제된 메시지입니다.
        </div>
      );
    }

    if (message.messageType === "IMAGE") {
      const attachment = message.attachments?.[0];
      if (!attachment) {
        return (
          <div className="flex h-36 w-56 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-100 to-blue-200 text-blue-400">
            <ImageIcon size={28} />
          </div>
        );
      }

      return (
        <a href={attachment.fileUrl} target="_blank" rel="noopener noreferrer">
          {/* eslint-disable-next-line @next/next/no-img-element -- 채팅 첨부 이미지는 S3 URL이라 next/image 설정 대상이 아니다 */}
          <img
            src={attachment.fileUrl}
            alt={attachment.originalFileName}
            className="h-36 w-56 rounded-2xl object-cover"
          />
        </a>
      );
    }

    if (message.messageType === "FILE") {
      const attachment = message.attachments?.[0];
      if (!attachment) return null;

      return (
        <a
          href={attachment.fileUrl}
          target="_blank"
          rel="noopener noreferrer"
          className={`flex w-64 items-center gap-3 rounded-2xl border px-3 py-2.5 text-sm hover:bg-gray-50 ${
            isOwn ? "border-primary/30" : "border-gray-200"
          }`}
        >
          <FileText size={20} className="shrink-0 text-gray-400" />
          <div className="min-w-0 flex-1">
            <p className="truncate font-medium text-gray-800">{attachment.originalFileName}</p>
            <p className="text-xs text-gray-400">{formatFileSize(attachment.fileSize)}</p>
          </div>
        </a>
      );
    }

    if (message.messageType === "CERTIFICATION_SHARE" && message.sharedCertification) {
      const cert = message.sharedCertification;
      return (
        <div className="w-64 rounded-2xl border border-gray-200 bg-surface p-3">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gray-100 text-xs font-semibold text-gray-500">
              {cert.authorNickname.slice(0, 1)}
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-gray-900">{cert.authorNickname}님의 인증글</p>
              <p className="truncate text-xs text-gray-400">{cert.challengeTitle}</p>
            </div>
          </div>
          {cert.thumbnailUrl && (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={cert.thumbnailUrl} alt="" className="mt-2 h-36 w-full rounded-xl object-cover" />
          )}
          <p className="mt-2 text-xs text-gray-400">인증일: {cert.certifiedAt}</p>
          <button type="button" className="mt-2 text-xs font-semibold text-primary hover:underline">
            인증글 보기 &gt;
          </button>
        </div>
      );
    }

    return (
      <div
        className={`max-w-xs rounded-2xl px-4 py-2.5 text-sm ${
          isOwn ? "bg-primary text-white" : "bg-gray-100 text-gray-800"
        }`}
      >
        {message.content}
      </div>
    );
  };

  return (
    <div className={`group flex gap-2 ${isOwn ? "justify-end" : "justify-start"}`}>
      {!isOwn && (
        <div className="w-8 shrink-0">
          {showSenderInfo && (
            <div
              className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold ${avatarColor.bg} ${avatarColor.text}`}
            >
              {message.senderNickname.slice(0, 1)}
            </div>
          )}
        </div>
      )}

      <div className={`flex max-w-[70%] flex-col ${isOwn ? "items-end" : "items-start"}`}>
        {!isOwn && isChallenge && showSenderInfo && (
          <div className="mb-1 flex items-center gap-1 px-1">
            <span className="text-xs font-medium text-gray-600">{message.senderNickname}</span>
            {message.senderBadgeApproved && (
              <BadgeCheck size={14} className="fill-primary stroke-white" aria-label="우수 인증자" />
            )}
          </div>
        )}

        {message.deletedAt && (
          <p className="mb-1 px-1 text-xs text-gray-400">{message.senderNickname}님이 메시지를 삭제했습니다.</p>
        )}

        <div className={`flex items-end gap-1.5 ${isOwn ? "flex-row-reverse" : ""}`}>
          {canDelete && (
            <button
              type="button"
              onClick={handleDelete}
              disabled={isDeletePending}
              aria-label="메시지 삭제"
              className="mb-1 shrink-0 rounded-lg p-1 text-gray-300 opacity-0 transition-opacity hover:bg-gray-100 hover:text-danger group-hover:opacity-100 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Trash2 size={14} />
            </button>
          )}
          {renderContent()}
          {readLabel && (
            <div className="flex shrink-0 flex-col items-end text-[11px] text-gray-400">
              <span>{readLabel}</span>
              <span>{time}</span>
            </div>
          )}
          {!readLabel && <span className="shrink-0 text-[11px] text-gray-400">{time}</span>}
        </div>
      </div>
    </div>
  );
};

export default MessageBubble;
