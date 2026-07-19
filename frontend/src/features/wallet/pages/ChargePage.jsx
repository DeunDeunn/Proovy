"use client";

import { useMemo, useState } from "react";
import Script from "next/script";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

import { useRequestCharge, useWallet } from "../hooks";
import { formatCurrency } from "../format";
import { NAVERPAY_MERCHANT_PAY_KEY_STORAGE_KEY, NAVERPAY_SDK_URL, createNaverPay } from "../naverpay";

const MIN_AMOUNT = 1_000;
const MAX_AMOUNT = 50_000;
const UNIT = 1_000;
const PRESET_AMOUNTS = [1_000, 3_000, 5_000, 10_000, 30_000, 50_000];
const INCREMENTS = [1_000, 5_000, 10_000];

const clampAmount = (value) => Math.min(MAX_AMOUNT, Math.max(0, value));

const ChargePage = () => {
  const { data: wallet, isLoading, error } = useWallet();
  const [amount, setAmount] = useState(20_000);
  const requestChargeMutation = useRequestCharge();

  const isValidAmount = amount >= MIN_AMOUNT && amount <= MAX_AMOUNT && amount % UNIT === 0;
  const isPresetSelected = PRESET_AMOUNTS.includes(amount);

  const projected = useMemo(() => {
    if (!wallet) return null;
    return {
      chargedBalance: (wallet.chargedBalance ?? 0) + amount,
      rewardBalance: wallet.rewardBalance ?? 0,
      lockedBalance: (wallet.lockedChargedBalance ?? 0) + (wallet.lockedRewardBalance ?? 0),
    };
  }, [wallet, amount]);

  const totalAfterCharge = projected
    ? projected.chargedBalance + projected.rewardBalance + projected.lockedBalance
    : 0;

  const handlePay = () => {
    if (!isValidAmount) return;

    requestChargeMutation.mutate(amount, {
      onSuccess: (chargeResponse) => {
        sessionStorage.setItem(NAVERPAY_MERCHANT_PAY_KEY_STORAGE_KEY, chargeResponse.merchantPayKey);

        const oPay = createNaverPay();
        oPay.open({
          merchantPayKey: chargeResponse.merchantPayKey,
          productName: chargeResponse.productName,
          productCount: chargeResponse.productCount,
          totalPayAmount: chargeResponse.totalPayAmount,
          taxScopeAmount: chargeResponse.taxScopeAmount,
          taxExScopeAmount: chargeResponse.taxExScopeAmount,
          returnUrl: chargeResponse.returnUrl,
        });
      },
    });
  };

  if (isLoading) return <Loading label="지갑 정보를 불러오는 중..." />;
  if (error) return <ErrorMessage error={error} />;

  return (
    <div>
      <Script src={NAVERPAY_SDK_URL} strategy="afterInteractive" />
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900">캐시 충전</h1>
        <p className="mt-1 text-sm text-gray-500">
          네이버페이로 간편하게 캐시를 충전하고 챌린지에 참여하세요.
        </p>
      </div>

      <div className="grid grid-cols-3 gap-8">
        <div className="col-span-2 space-y-8">
          <Card className="py-8">
            <h2 className="mb-5 font-semibold text-gray-900">충전 금액 선택</h2>

            <div className="grid grid-cols-4 gap-4">
              {PRESET_AMOUNTS.map((preset) => (
                <button
                  key={preset}
                  onClick={() => setAmount(preset)}
                  className={`rounded-lg border px-3 py-4 text-sm font-medium transition-colors ${
                    amount === preset
                      ? "border-primary bg-primary-light text-primary"
                      : "border-gray-200 text-gray-700 hover:bg-gray-50"
                  }`}
                >
                  {formatCurrency(preset).replace("₩ ", "")}원
                </button>
              ))}
              <div
                className={`col-span-2 flex items-center gap-2 rounded-lg border px-3 py-2 ${
                  !isPresetSelected ? "border-primary" : "border-gray-200"
                }`}
              >
                <span className="shrink-0 text-sm text-gray-500">직접 입력</span>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(clampAmount(Number(e.target.value) || 0))}
                  className="w-full min-w-0 border-none bg-transparent text-right text-sm font-medium text-gray-900 outline-none [-moz-appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
                />
                <span className="shrink-0 text-sm text-gray-500">원</span>
              </div>
            </div>

            <div className="mt-4 flex gap-2">
              {INCREMENTS.map((inc) => (
                <button
                  key={inc}
                  onClick={() => setAmount((prev) => clampAmount(prev + inc))}
                  className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
                >
                  +{formatCurrency(inc).replace("₩ ", "")}
                </button>
              ))}
            </div>

            <p className="mt-4 text-xs text-gray-400">
              최소 1,000원 ~ 최대 50,000원 (1,000원 단위)
            </p>

            {!isValidAmount && (
              <p className="mt-2 text-xs text-danger">
                충전 금액은 1,000원 이상 50,000원 이하, 1,000원 단위여야 합니다.
              </p>
            )}
          </Card>

          <Card className="py-8">
            <h2 className="mb-5 font-semibold text-gray-900">결제 수단</h2>
            <div className="flex items-center justify-between rounded-lg border border-primary px-5 py-4">
              <div className="flex items-center gap-2">
                <span className="rounded bg-green-500 px-2 py-1 text-xs font-bold text-white">
                  N
                </span>
                <span className="font-medium text-gray-900">Pay 네이버페이</span>
              </div>
              <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary text-xs text-white">
                ✓
              </span>
            </div>
            <p className="mt-4 text-xs text-gray-400">다른 결제 수단 준비 중이에요.</p>
          </Card>
        </div>

        <div>
          <Card className="sticky top-6 py-8">
            <h2 className="mb-5 font-semibold text-gray-900">결제 요약</h2>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">충전 금액</span>
                <span className="font-medium text-gray-900">{formatCurrency(amount)}</span>
              </div>
            </div>

            <div className="my-5 border-t border-gray-100" />

            <div className="flex items-center justify-between">
              <span className="font-medium text-gray-700">결제 금액</span>
              <span className="text-lg font-bold text-gray-900">{formatCurrency(amount)}</span>
            </div>

            <div className="my-5 border-t border-gray-100" />

            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">충전 후 총 보유 캐시</span>
                <span className="font-semibold text-success">{formatCurrency(totalAfterCharge)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">충전 캐시</span>
                <span className="text-gray-700">{formatCurrency(projected?.chargedBalance)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">리워드 캐시</span>
                <span className="text-gray-700">{formatCurrency(projected?.rewardBalance)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">홀딩 캐시</span>
                <span className="text-gray-700">{formatCurrency(projected?.lockedBalance)}</span>
              </div>
            </div>

            <Button
              variant="primary"
              className="mt-7 w-full py-3 text-base"
              disabled={!isValidAmount || requestChargeMutation.isPending}
              onClick={handlePay}
            >
              {formatCurrency(amount)} 결제하기
            </Button>

            {requestChargeMutation.isError && (
              <div className="mt-3">
                <ErrorMessage error={requestChargeMutation.error} />
              </div>
            )}

            <p className="mt-3 text-center text-xs text-gray-400">
              결제 시 네이버페이 약관 및 개인정보 처리방침에 동의합니다.
            </p>

            <div className="mt-5 rounded-lg bg-gray-50 p-4 text-xs text-gray-500">
              <p className="mb-2 font-medium text-gray-600">안내 사항</p>
              <ul className="list-disc space-y-1.5 pl-4">
                <li>충전한 캐시는 챌린지 참가비 결제에 사용할 수 있어요.</li>
                <li>충전 후 7일이 지나야 출금이 가능해요.</li>
                <li>출금 시 충전 캐시는 5% 수수료가 부과돼요.</li>
              </ul>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default ChargePage;
