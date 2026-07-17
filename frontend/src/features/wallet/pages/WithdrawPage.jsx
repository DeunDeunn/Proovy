"use client";

import { useMemo, useState } from "react";
import { Check } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

import { useApplyWithdrawal, useWallet, useWithdrawableAmount } from "../hooks";
import { formatCurrency } from "../format";

// 실제 수수료는 서버(applyWithdrawal 응답의 feeAmount)에서 계산됨 - 여기선 신청 전 미리보기용
const FEE_RATES = { CHARGED: 0.05, REWARD: 0.01 };
const QUICK_ADD_AMOUNTS = [10_000, 50_000];

const SourceOption = ({ label, withdrawable, total, note, selected, disabled, onSelect }) => (
  <button
    onClick={onSelect}
    disabled={disabled}
    className={`flex-1 rounded-xl border p-4 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
      selected ? "border-primary bg-primary-light" : "border-gray-200 hover:bg-gray-50"
    }`}
  >
    <div className="flex items-center justify-between">
      <span className="text-sm font-medium text-gray-700">{label}</span>
      {selected && (
        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary text-white">
          <Check size={12} />
        </span>
      )}
    </div>
    <div className="mt-2 text-xl font-bold text-gray-900">{formatCurrency(withdrawable)}</div>
    <div className="mt-1 text-xs text-gray-500">{note ?? `(전체: ${formatCurrency(total)})`}</div>
  </button>
);

const WithdrawPage = () => {
  const { data: wallet, isLoading: walletLoading, error: walletError } = useWallet();
  const {
    data: withdrawable,
    isLoading: withdrawableLoading,
    error: withdrawableError,
  } = useWithdrawableAmount();

  const [sourceType, setSourceType] = useState("REWARD");
  const [amount, setAmount] = useState(0);
  const [bankName, setBankName] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [accountHolderName, setAccountHolderName] = useState("");

  const applyWithdrawalMutation = useApplyWithdrawal();

  const maxWithdrawable =
    sourceType === "CHARGED"
      ? withdrawable?.chargedWithdrawableAmount ?? 0
      : withdrawable?.rewardWithdrawableAmount ?? 0;

  const isValidAmount = amount > 0 && amount <= maxWithdrawable;

  const { feeAmount, netAmount } = useMemo(() => {
    const rate = FEE_RATES[sourceType];
    const fee = Math.floor(amount * rate);
    return { feeAmount: fee, netAmount: amount - fee };
  }, [amount, sourceType]);

  const handleSelectSource = (type) => {
    setSourceType(type);
    setAmount(0);
  };

  const handleSubmit = () => {
    if (!isValidAmount || !bankName || !accountNumber || !accountHolderName) return;

    applyWithdrawalMutation.mutate({
      sourceType,
      amount,
      bankName,
      accountNumber,
      accountHolderName,
    });
  };

  if (walletLoading || withdrawableLoading) return <Loading label="출금 정보를 불러오는 중..." />;
  if (walletError) return <ErrorMessage error={walletError} />;
  if (withdrawableError) return <ErrorMessage error={withdrawableError} />;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900">출금 신청</h1>
        <p className="mt-1 text-sm text-gray-500">보유한 캐시를 계좌로 출금할 수 있습니다.</p>
      </div>

      <div className="grid grid-cols-3 gap-6">
        <div className="col-span-2 space-y-6">
          <Card>
            <h2 className="mb-4 font-semibold text-gray-900">출금할 캐시 선택</h2>
            <div className="flex gap-4">
              <SourceOption
                label="충전 캐시"
                withdrawable={withdrawable?.chargedWithdrawableAmount}
                total={wallet?.chargedBalance}
                note={`(전체: ${formatCurrency(wallet?.chargedBalance)}) · 7일 미경과분 제외`}
                selected={sourceType === "CHARGED"}
                onSelect={() => handleSelectSource("CHARGED")}
              />
              <SourceOption
                label="리워드 캐시"
                withdrawable={withdrawable?.rewardWithdrawableAmount}
                total={wallet?.rewardBalance}
                selected={sourceType === "REWARD"}
                onSelect={() => handleSelectSource("REWARD")}
              />
            </div>
          </Card>

          <Card>
            <h2 className="mb-3 font-semibold text-gray-900">출금 금액</h2>
            <div className="flex items-center gap-2">
              <input
                type="number"
                value={amount}
                onChange={(e) =>
                  setAmount(Math.max(0, Math.min(maxWithdrawable, Number(e.target.value) || 0)))
                }
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-right text-sm outline-none focus:border-primary [-moz-appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
              />
              <span className="text-sm text-gray-500">원</span>
              {QUICK_ADD_AMOUNTS.map((inc) => (
                <button
                  key={inc}
                  onClick={() =>
                    setAmount((prev) => Math.min(maxWithdrawable, prev + inc))
                  }
                  className="shrink-0 rounded-lg border border-gray-200 px-3 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50"
                >
                  +{inc / 10_000}만원
                </button>
              ))}
              <button
                onClick={() => setAmount(maxWithdrawable)}
                className="shrink-0 rounded-lg border border-gray-200 px-3 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50"
              >
                전액
              </button>
            </div>
            <p className="mt-2 text-xs text-gray-400">
              출금 가능 금액: {formatCurrency(maxWithdrawable)}
            </p>
            {amount > 0 && !isValidAmount && (
              <p className="mt-1 text-xs text-danger">출금 가능 금액을 초과했어요.</p>
            )}
          </Card>

          <Card>
            <h2 className="mb-4 font-semibold text-gray-900">계좌 정보</h2>
            <div className="space-y-3">
              <select
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-700 outline-none focus:border-primary"
              >
                <option value="">은행 선택</option>
                <option value="국민은행">국민은행</option>
                <option value="신한은행">신한은행</option>
                <option value="우리은행">우리은행</option>
                <option value="하나은행">하나은행</option>
                <option value="카카오뱅크">카카오뱅크</option>
                <option value="토스뱅크">토스뱅크</option>
              </select>
              <input
                type="text"
                placeholder="예금주"
                value={accountHolderName}
                onChange={(e) => setAccountHolderName(e.target.value)}
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-primary"
              />
              <input
                type="text"
                placeholder="- 없이 입력해주세요"
                value={accountNumber}
                onChange={(e) => setAccountNumber(e.target.value)}
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-primary"
              />
            </div>
          </Card>
        </div>

        <div>
          <Card className="sticky top-6">
            <h2 className="mb-4 font-semibold text-gray-900">출금 요약</h2>

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">출금 캐시</span>
                <span className="font-medium text-gray-900">
                  {sourceType === "CHARGED" ? "충전 캐시" : "리워드 캐시"}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">출금 금액</span>
                <span className="font-medium text-gray-900">{formatCurrency(amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">
                  수수료 ({FEE_RATES[sourceType] * 100}%)
                </span>
                <span className="font-medium text-danger">-{formatCurrency(feeAmount)}</span>
              </div>
            </div>

            <div className="my-4 border-t border-gray-100" />

            <div className="flex items-center justify-between">
              <span className="font-medium text-gray-700">실수령액</span>
              <span className="text-xl font-bold text-primary">{formatCurrency(netAmount)}</span>
            </div>

            <div className="my-4 rounded-lg bg-gray-50 p-3 text-xs text-gray-500">
              <p className="mb-1 font-medium text-gray-600">안내 사항</p>
              <ul className="list-disc space-y-1 pl-4">
                <li>출금 처리는 평일 기준 7일 이내에 처리됩니다.</li>
                <li>출금 신청은 평일 09:00 ~ 18:00에 처리됩니다.</li>
              </ul>
            </div>

            <Button
              variant="primary"
              className="w-full py-3 text-base"
              disabled={
                !isValidAmount ||
                !bankName ||
                !accountNumber ||
                !accountHolderName ||
                applyWithdrawalMutation.isPending
              }
              onClick={handleSubmit}
            >
              {formatCurrency(netAmount)} 출금 신청하기
            </Button>

            {applyWithdrawalMutation.isError && (
              <div className="mt-3">
                <ErrorMessage error={applyWithdrawalMutation.error} />
              </div>
            )}

            {applyWithdrawalMutation.isSuccess && (
              <p className="mt-3 text-center text-xs text-success">출금 신청이 접수됐어요.</p>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
};

export default WithdrawPage;
