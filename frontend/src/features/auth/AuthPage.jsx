"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";

const GoogleIcon = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
    <path
      fill="#4285F4"
      d="M19.8 10.23c0-.68-.06-1.36-.18-2.02H10.2v3.83h5.4a4.62 4.62 0 0 1-2 3.03v2.5h3.23c1.9-1.75 2.97-4.32 2.97-7.34z"
    />
    <path
      fill="#34A853"
      d="M10.2 20c2.7 0 4.97-.89 6.63-2.42l-3.23-2.5c-.9.6-2.05.96-3.4.96a5.9 5.9 0 0 1-5.54-4.1H1.32v2.6A10.2 10.2 0 0 0 10.2 20z"
    />
    <path
      fill="#FBBC05"
      d="M4.66 12a6.1 6.1 0 0 1 0-3.9V5.5H1.32a10.2 10.2 0 0 0 0 9.1l3.34-2.6z"
    />
    <path
      fill="#EA4335"
      d="M10.2 3.9c1.47 0 2.79.5 3.83 1.5l2.87-2.86C15.16.94 12.9 0 10.2 0A10.2 10.2 0 0 0 1.32 5.5l3.34 2.6a5.9 5.9 0 0 1 5.54-4.2z"
    />
  </svg>
);

const KakaoIcon = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
    <path
      fill="#191600"
      d="M10 2.5c-4.42 0-8 2.83-8 6.32 0 2.24 1.48 4.22 3.72 5.35-.16.6-.6 2.22-.68 2.57-.1.42.16.42.33.3.14-.1 2.2-1.47 3.09-2.07.5.07 1.01.11 1.54.11 4.42 0 8-2.83 8-6.32 0-3.5-3.58-6.32-8-6.32z"
    />
  </svg>
);

const OAUTH_PROVIDERS = [
  {
    id: "google",
    label: "Google로 시작하기",
    icon: GoogleIcon,
    className: "border border-gray-300 bg-white text-gray-700 hover:bg-gray-50",
  },
  {
    id: "kakao",
    label: "Kakao로 시작하기",
    icon: KakaoIcon,
    className: "bg-[#FEE500] text-[#191600] hover:bg-[#f5dc00]",
  },
];

// OAuth 인증은 백엔드→소셜 로그인 창→백엔드→/auth/callback으로 이어지는 풀 페이지 리다이렉트라
// URL 쿼리로 목적지를 들고 다닐 수 없어서, 같은 탭에 남는 sessionStorage에 잠깐 저장해둔다.
export const POST_LOGIN_REDIRECT_KEY = "postLoginRedirect";

const handleOAuthLogin = (provider, redirect) => {
  if (redirect) {
    sessionStorage.setItem(POST_LOGIN_REDIRECT_KEY, redirect);
  }
  window.location.href = `/api/oauth2/authorization/${provider}`;
};

const AuthPageContent = () => {
  const redirect = useSearchParams().get("redirect");

  return (
    <div className="flex h-full flex-col items-center justify-center gap-8">
      <div className="flex flex-col items-center gap-2">
        <h1 className="text-xl font-bold text-gray-900">Proovy 로그인</h1>
        <p className="text-sm text-gray-500">소셜 계정으로 로그인하세요</p>
      </div>

      <div className="flex w-full max-w-sm flex-col gap-3">
        {OAUTH_PROVIDERS.map(({ id, label, icon: Icon, className }) => (
          <button
            key={id}
            type="button"
            onClick={() => handleOAuthLogin(id, redirect)}
            className={`relative flex cursor-pointer items-center rounded-full px-5 py-3.5 text-sm font-semibold transition-colors ${className}`}
          >
            <span className="absolute left-5">
              <Icon />
            </span>
            <span className="flex-1 text-center">{label}</span>
          </button>
        ))}
      </div>
    </div>
  );
};

const AuthPage = () => (
  <Suspense fallback={null}>
    <AuthPageContent />
  </Suspense>
);

export default AuthPage;
