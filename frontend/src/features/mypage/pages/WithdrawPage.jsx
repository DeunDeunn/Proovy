"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useMe, useWithdraw } from "@/features/auth/hooks";

const PROVIDER_LABEL = { google: "Google", kakao: "Kakao" };
const CASH_REMAINING_CODE = "U006";

const WithdrawPage = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const { data: me, isLoading } = useMe();
  const withdrawMutation = useWithdraw();

  const reauthParam = searchParams.get("reauth");
  const isReauthed = reauthParam === "true";
  const reauthFailed = reauthParam === "false";

  if (isLoading) return <Loading />;
  if (!me) return null;

  const providerKey = me.provider?.toLowerCase();
  const providerLabel = PROVIDER_LABEL[providerKey] ?? me.provider;

  const handleReauth = () => {
    window.location.href = `/api/auth/${providerKey}/reauth`;
  };

  const handleWithdraw = () => {
    withdrawMutation.mutate(undefined, {
      onSuccess: () => {
        queryClient.removeQueries({ queryKey: ["auth", "me"] });
        router.replace("/");
      },
    });
  };

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-bold text-gray-900">회원탈퇴</h1>

      <Card className="flex flex-col gap-4">
        <p className="text-sm text-gray-600">
          탈퇴하면 계정 정보와 활동 내역이 모두 삭제되며 복구할 수 없어요. 계속하려면 본인 확인이 필요합니다.
        </p>

        {reauthFailed && <ErrorMessage error={{ message: "본인 확인에 실패했어요. 다시 시도해주세요." }} />}

        {!isReauthed ? (
          <Button variant="outline" onClick={handleReauth} className="self-start">
            {providerLabel} 계정으로 본인 확인하기
          </Button>
        ) : (
          <div className="flex flex-col items-start gap-2">
            <p className="text-sm text-primary">본인 확인이 완료됐어요.</p>
            <Button variant="danger" onClick={handleWithdraw} disabled={withdrawMutation.isPending}>
              {withdrawMutation.isPending ? "탈퇴 처리 중..." : "탈퇴하기"}
            </Button>
          </div>
        )}

        {withdrawMutation.isError && (
          <div className="flex flex-col gap-2">
            <ErrorMessage error={withdrawMutation.error} />
            {withdrawMutation.error?.code === CASH_REMAINING_CODE && (
              <Link
                href="/wallet/withdraw"
                className="self-start rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50"
              >
                캐시 출금하러 가기
              </Link>
            )}
          </div>
        )}
      </Card>
    </div>
  );
};

export default WithdrawPage;
