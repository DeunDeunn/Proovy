"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";

import { useConfirmNaverPayCharge } from "../hooks";
import { NAVERPAY_MERCHANT_PAY_KEY_STORAGE_KEY } from "../naverpay";

const ChargeReturnPage = () => {
  const searchParams = useSearchParams();
  const paymentId = searchParams.get("paymentId");
  const resultCode = searchParams.get("resultCode");
  const resultMessage = searchParams.get("resultMessage");
  // 네이버페이가 returnUrl에 실어 보내주는 결과 코드 - Success가 아니면 취소/실패/만료 등이라
  // 굳이 백엔드 콜백을 호출할 필요 없이 그 자리에서 바로 실패로 안내한다.
  const isKnownFailure = resultCode !== "Success";

  const confirmMutation = useConfirmNaverPayCharge();
  const hasRequestedRef = useRef(false);

  useEffect(() => {
    if (hasRequestedRef.current || isKnownFailure) return;
    hasRequestedRef.current = true;

    const merchantPayKey = sessionStorage.getItem(NAVERPAY_MERCHANT_PAY_KEY_STORAGE_KEY);

    confirmMutation.mutate(
      { merchantPayKey, paymentId },
      { onSuccess: () => sessionStorage.removeItem(NAVERPAY_MERCHANT_PAY_KEY_STORAGE_KEY) }
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paymentId, isKnownFailure]);

  return (
    <div className="mx-auto max-w-md py-16">
      <Card className="text-center">
        {isKnownFailure ? (
          <>
            <p className="text-lg font-semibold text-danger">충전이 실패했어요.</p>
            <p className="mt-2 text-sm text-gray-500">
              {resultMessage || "결제가 완료되지 않았어요. 다시 시도해주세요."}
            </p>
          </>
        ) : confirmMutation.isSuccess ? (
          confirmMutation.data?.status === "COMPLETED" ? (
            <>
              <p className="text-lg font-semibold text-success">충전이 완료됐어요.</p>
              <p className="mt-2 text-sm text-gray-500">지갑에서 충전된 캐시를 확인해보세요.</p>
            </>
          ) : (
            <>
              <p className="text-lg font-semibold text-danger">충전이 실패했어요.</p>
              <p className="mt-2 text-sm text-gray-500">결제가 승인되지 않았어요. 다시 시도해주세요.</p>
            </>
          )
        ) : confirmMutation.isError ? (
          <ErrorMessage error={confirmMutation.error} />
        ) : (
          <Loading label="결제 결과를 확인하는 중..." />
        )}

        <Link href="/wallet">
          <Button variant="primary" className="mt-6 w-full py-3">
            지갑으로 이동
          </Button>
        </Link>
      </Card>
    </div>
  );
};

export default ChargeReturnPage;
