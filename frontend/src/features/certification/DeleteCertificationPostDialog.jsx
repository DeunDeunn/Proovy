"use client";

import { useEffect, useRef } from "react";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";

const DeleteCertificationPostDialog = ({ error, isDeleting, onClose, onDelete }) => {
  const dialogRef = useRef(null);

  useEffect(() => {
    dialogRef.current?.querySelector('[data-delete-dialog-action="cancel"]')?.focus();
  }, []);

  const handleKeyDown = (event) => {
    if (event.key === "Escape" && !isDeleting) {
      event.preventDefault();
      onClose();
      return;
    }

    if (event.key !== "Tab") return;

    const firstButton = dialogRef.current?.querySelector('[data-delete-dialog-action="cancel"]');
    const lastButton = dialogRef.current?.querySelector('[data-delete-dialog-action="delete"]');
    if (!firstButton || !lastButton) return;

    if (event.shiftKey && document.activeElement === firstButton) {
      event.preventDefault();
      lastButton.focus();
    } else if (!event.shiftKey && document.activeElement === lastButton) {
      event.preventDefault();
      firstButton.focus();
    }
  };

  return (
    <div
      role="presentation"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4"
    >
      <section
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-certification-post-dialog-title"
        aria-describedby="delete-certification-post-dialog-description"
        onKeyDown={handleKeyDown}
        className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl"
      >
        <h2 id="delete-certification-post-dialog-title" className="text-lg font-bold text-gray-900">
          인증 게시글을 삭제할까요?
        </h2>
        <p
          id="delete-certification-post-dialog-description"
          className="mt-2 text-sm leading-6 text-gray-600"
        >
          삭제한 인증글은 복구할 수 없으며, 삭제해도 오늘은 다시 인증할 수 없습니다.
        </p>

        {error && (
          <div className="mt-4">
            <ErrorMessage error={error} />
          </div>
        )}

        <div className="mt-6 flex justify-end gap-2">
          <Button
            data-delete-dialog-action="cancel"
            type="button"
            variant="outline"
            className="!rounded-full"
            onClick={onClose}
            disabled={isDeleting}
          >
            취소
          </Button>
          <Button
            data-delete-dialog-action="delete"
            type="button"
            variant="danger"
            className="!rounded-full"
            onClick={onDelete}
            disabled={isDeleting}
          >
            {isDeleting ? "삭제 중..." : "삭제"}
          </Button>
        </div>
      </section>
    </div>
  );
};

export default DeleteCertificationPostDialog;
