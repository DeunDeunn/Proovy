"use client";

import RequireAuth from "@/components/auth/RequireAuth";
import { useMe } from "@/features/auth/hooks";

const AdminOnly = ({ children }) => {
  const { data: me } = useMe();

  if (me?.role !== "ADMIN") {
    return (
      <div className="flex flex-col items-center gap-2 py-24 text-center">
        <p className="text-sm font-semibold text-gray-900">접근 권한이 없어요</p>
        <p className="text-sm text-gray-500">관리자만 볼 수 있는 화면이에요.</p>
      </div>
    );
  }

  return children;
};

/**
 * 로그인 + ADMIN role을 모두 요구하는 가드.
 * 로그인 여부는 RequireAuth가, role 체크는 AdminOnly가 담당한다.
 */
const RequireAdmin = ({ children }) => (
  <RequireAuth>
    <AdminOnly>{children}</AdminOnly>
  </RequireAuth>
);

export default RequireAdmin;
