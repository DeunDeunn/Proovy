"use client";

import { useRouter } from "next/navigation";

import Button from "@/components/ui/Button";

const LoginRequiredModal = ({ description = "로그인 후 이용할 수 있어요." }) => {
  const router = useRouter();

  return (
    <div
      role="presentation"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4"
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-required-title"
        className="w-full max-w-sm rounded-xl bg-white p-6 text-center shadow-xl"
      >
        <h2 id="login-required-title" className="text-lg font-bold text-gray-900">
          로그인이 필요합니다
        </h2>
        <p className="mt-2 text-sm leading-6 text-gray-600">{description}</p>

        <div className="mt-6 flex justify-center gap-2">
          <Button type="button" variant="outline" onClick={() => router.push("/")}>
            홈으로
          </Button>
          <Button type="button" onClick={() => router.push("/login")}>
            로그인하기
          </Button>
        </div>
      </section>
    </div>
  );
};

export default LoginRequiredModal;
