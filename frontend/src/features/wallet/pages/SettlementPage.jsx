"use client";

import Link from "next/link";
import { ChevronRight } from "lucide-react";

import Card from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";

import { formatCurrency, formatDate } from "../format";

// TODO: 정산 내역 목록 API 연동 필요 (예: GET /wallets/settlements 등) - 백엔드 준비 후 useQuery로 교체
const settlements = [
  {
    challengeId: 1,
    title: "매일 아침 6시 기상 인증 챌린지",
    isSuccess: true,
    settledAt: "2026-07-10T09:00:00",
    profitAmount: 12000,
  },
  {
    challengeId: 2,
    title: "하루 만보 걷기",
    isSuccess: false,
    settledAt: "2026-07-05T09:00:00",
    profitAmount: 0,
  },
  {
    challengeId: 3,
    title: "퇴근 후 헬스장 30일 인증",
    isSuccess: true,
    settledAt: "2026-06-28T09:00:00",
    profitAmount: 8500,
  },
];

const SettlementCard = ({ settlement }) => (
  <Link href={`/wallet/settlements/${settlement.challengeId}`}>
    <Card className="transition-shadow hover:shadow-md">
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
        <div className="shrink-0 text-right">
          <p className="text-xs text-gray-500">정산 수익</p>
          <p className="font-semibold text-success">+{formatCurrency(settlement.profitAmount)}</p>
        </div>
        <ChevronRight size={18} className="shrink-0 text-gray-400" />
      </div>
    </Card>
  </Link>
);

const SettlementPage = () => {
  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900">정산 내역</h1>
        <p className="mt-1 text-sm text-gray-500">참여한 챌린지의 정산 결과를 확인하세요.</p>
      </div>

      {settlements.length === 0 ? (
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
