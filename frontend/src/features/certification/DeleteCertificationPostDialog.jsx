"use client";

import Button from "@/components/ui/Button";

const DeleteCertificationPostDialog = ({ isDeleting, onClose, onDelete }) => (
  <div
    role="presentation"
    className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4"
  >
    <section
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-certification-post-dialog-title"
      aria-describedby="delete-certification-post-dialog-description"
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

      <div className="mt-6 flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onClose} disabled={isDeleting}>
          취소
        </Button>
        <Button type="button" variant="danger" onClick={onDelete} disabled={isDeleting}>
          {isDeleting ? "삭제 중..." : "삭제"}
        </Button>
      </div>
    </section>
  </div>
);

export default DeleteCertificationPostDialog;
