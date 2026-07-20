"use client";

import { Wallet, Gift, Lock, ChevronRight, Info } from "lucide-react";

import Card from "@/components/ui/Card";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Badge from "@/components/ui/Badge";
import Tooltip from "@/components/ui/Tooltip";

import { useTransactions, useWallet } from "../hooks";
import { formatCurrency, formatDate, formatSignedAmount, getTransactionBadge } from "../format";

const RECENT_TRANSACTIONS_COUNT = 5;

const SummaryCard = ({ icon: Icon, label, amount, badge, highlight }) => (
  <Card className={highlight ? "bg-amber-50" : ""}>
    <div className="flex items-center gap-2 text-sm text-gray-600">
      <span
        className={`flex h-8 w-8 items-center justify-center rounded-full ${
          highlight ? "bg-amber-100 text-amber-700" : "bg-primary-light text-primary"
        }`}
      >
        <Icon size={16} />
      </span>
      {label}
    </div>
    <div className="mt-3 text-2xl font-bold text-gray-900">{formatCurrency(amount)}</div>
    <div className="mt-1 text-xs text-gray-500">{badge}</div>
  </Card>
);

const WalletPage = () => {
  const { data: wallet, isLoading, error } = useWallet();
  const {
    data: transactionHistory,
    isLoading: transactionsLoading,
    error: transactionsError,
  } = useTransactions({ page: 0 });

  if (isLoading) return <Loading label="지갑 정보를 불러오는 중..." />;
  if (error) return <ErrorMessage error={error} />;

  const totalBalance = (wallet?.chargedBalance ?? 0) + (wallet?.rewardBalance ?? 0);
  const totalLocked = (wallet?.lockedChargedBalance ?? 0) + (wallet?.lockedRewardBalance ?? 0);
  const recentTransactions = (transactionHistory?.content ?? []).slice(0, RECENT_TRANSACTIONS_COUNT);

  return (
    <div>
      <div className="mb-6">
        <h1 className="flex items-center gap-1 text-xl font-bold text-gray-900">
          내 지갑
          <Tooltip
            text="충전한 캐시와 정산으로 받은 리워드 캐시를 한 곳에서 확인하고 관리할 수 있어요."
            position="bottom"
            align="start"
          >
            <Info size={16} className="text-gray-400" />
          </Tooltip>
        </h1>
        <p className="mt-1 text-sm text-gray-500">보유 중인 총 캐시와 이용 내역을 확인하세요.</p>
      </div>

      <Card className="mb-6">
        <div className="grid grid-cols-2 gap-6">
          <div>
            <div className="flex items-center gap-1 text-sm text-gray-500">
              실사용 가능 캐시
              <Tooltip text="챌린지 참가나 출금에 바로 사용할 수 있는 캐시예요. 홀딩 중인 캐시는 포함되지 않아요.">
                <Info size={14} className="text-gray-400" />
              </Tooltip>
            </div>
            <div className="mt-2 text-3xl font-bold text-gray-900">
              {formatCurrency(wallet?.availableBalance)}
            </div>
          </div>
          <div>
            <div className="flex items-center gap-1 text-sm text-gray-500">
              총 보유 캐시
              <Tooltip text="실사용 가능 캐시와 홀딩 캐시를 합한, 현재 보유 중인 전체 캐시예요.">
                <Info size={14} className="text-gray-400" />
              </Tooltip>
            </div>
            <div className="mt-2 text-3xl font-bold text-gray-900">
              {formatCurrency(totalBalance)}
            </div>
            <div className="mt-1">
              <span className="text-xs text-gray-500">(실사용 가능 캐시 + 홀딩 캐시)</span>
            </div>
          </div>
        </div>
      </Card>

      <div className="mb-6 grid grid-cols-3 gap-4">
        <SummaryCard
          icon={Wallet}
          label="충전 캐시"
          amount={wallet?.chargedBalance}
          badge="출금 가능"
        />
        <SummaryCard
          icon={Gift}
          label="리워드 캐시"
          amount={wallet?.rewardBalance}
          badge="출금 가능"
        />
        <SummaryCard
          icon={Lock}
          label="홀딩 캐시"
          amount={totalLocked}
          badge="출금 불가"
          highlight
        />
      </div>

      <Card>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-semibold text-gray-900">최근 캐시 이동 내역</h2>
          <button className="flex items-center text-xs font-medium text-primary">
            전체 내역 보기
            <ChevronRight size={14} />
          </button>
        </div>

        {transactionsLoading ? (
          <Loading label="거래 내역을 불러오는 중..." />
        ) : transactionsError ? (
          <ErrorMessage error={transactionsError} />
        ) : recentTransactions.length === 0 ? (
          <p className="py-8 text-center text-sm text-gray-500">아직 캐시 이동 내역이 없어요.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 text-left text-gray-500">
                <th className="pb-2 font-medium">날짜</th>
                <th className="pb-2 font-medium">구분</th>
                <th className="pb-2 text-right font-medium">금액</th>
                <th className="pb-2 text-right font-medium">잔액</th>
              </tr>
            </thead>
            <tbody>
              {recentTransactions.map((item) => {
                const badge = getTransactionBadge(item);
                const isSettled = item.status === "COMPLETED";
                return (
                  <tr key={item.id} className="border-b border-gray-50 last:border-0">
                    <td className="py-3 text-gray-500">{formatDate(item.createdAt)}</td>
                    <td className="py-3">
                      <Badge variant={badge.variant}>{badge.label}</Badge>
                    </td>
                    <td
                      className={`py-3 text-right font-medium ${
                        isSettled ? "text-gray-900" : "text-gray-400 line-through"
                      }`}
                    >
                      {formatSignedAmount(item.type, item.amount)}
                    </td>
                    <td className="py-3 text-right text-gray-500">
                      {isSettled ? formatCurrency(item.balanceAfter) : "-"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
};

export default WalletPage;
