"use client";

import Link from "next/link";
import { ChevronRight } from "lucide-react";

import Card from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";

import { useSettlementHistory } from "../hooks";
import { formatCurrency, formatDate } from "../format";

const SettlementCard = ({ settlement }) => {
  // 실패한 챌린지는 참가자 쪽 cash_transactions 기록이 없을 수 있어 상세 조회가 안 되므로
  // 목록에는 보여주되 상세 페이지로 이동은 막는다.
  const content = (
    <Card className={settlement.isSuccess ? "transition-shadow hover:shadow-md" : ""}>
      <div className="flex items-center gap-4">
        <div className="h-16 w-20 shrink-0 rounded-lg bg-gray-100" />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <h3 className="min-w-0 truncate font-semibold text-gray-900">{settlement.title}</h3>
            <span className="shrink-0">
              <Badge variant={settlement.isSuccess ? "success" : "danger"}>
                {settlement.isSuccess ? "성공" : "실패"}
              </Badge>
            </span>
          </div>
          <p className="mt-1 text-sm text-gray-500">정산일 {formatDate(settlement.settledAt)}</p>
        </div>
        {settlement.isSuccess && (
          <div className="shrink-0 text-right">
            <p className="text-xs text-gray-500">정산 수익</p>
            <p className="font-semibold text-success">+{formatCurrency(settlement.profitAmount)}</p>
          </div>
        )}
        {settlement.isSuccess && <ChevronRight size={18} className="shrink-0 text-gray-400" />}
      </div>
    </Card>
  );

  if (!settlement.isSuccess) {
    return content;
  }

  return <Link href={`/wallet/settlements/${settlement.challengeId}`}>{content}</Link>;
};

const SettlementPage = () => {
  const { data, isLoading, error } = useSettlementHistory({ page: 0 });
  const settlements = data?.content ?? [];

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900">정산 내역</h1>
        <p className="mt-1 text-sm text-gray-500">참여한 챌린지의 정산 결과를 확인하세요.</p>
      </div>

      {isLoading ? (
        <Loading label="정산 내역을 불러오는 중..." />
      ) : error ? (
        <ErrorMessage error={error} />
      ) : settlements.length === 0 ? (
        <Card>
          <p className="py-12 text-center text-sm text-gray-500">정산 완료된 챌린지가 없습니다.</p>
        </Card>
      ) : (
        <div className="space-y-3">
          {settlements.map((settlement) => (
            <SettlementCard key={settlement.challengeId} settlement={settlement} />
          ))}
        </div>
      )}
    </div>
  );
};

export default SettlementPage;
