"use client";

import { ArrowRight, ShoppingBag, Sparkles, Ticket } from "lucide-react";
import Link from "next/link";

import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import ActiveTicketCard from "@/features/ai/components/ActiveTicketCard";
import TicketHistoryTable from "@/features/ai/components/TicketHistoryTable";
import { useActiveAiTicket, useAiTicketHistory } from "@/features/ai/hooks";

const TicketPage = () => {
  const { data: activeTicket, isLoading: activeLoading, error: activeError } =
    useActiveAiTicket();
  const { data: history, isLoading: historyLoading, error: historyError } =
    useAiTicketHistory({ page: 0, size: 10 });

  if (activeLoading) return <Loading label="AI 티켓 정보를 불러오는 중..." />;
  if (activeError) return <ErrorMessage error={activeError} />;

  return (
    <div className="mx-auto max-w-6xl">
      <header className="mb-6 flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-light text-primary">
              <Sparkles size={19} />
            </span>
            <h1 className="text-xl font-bold text-gray-900">AI 티켓 관리</h1>
          </div>
          <p className="mt-2 text-sm text-gray-500">
            현재 이용권과 AI 검수 사용 내역을 확인하세요.
          </p>
        </div>
        <Link
          href="/mypage/tickets/store"
          className="flex shrink-0 items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-hover"
        >
          <ShoppingBag size={17} />
          스토어
          <ArrowRight size={15} />
        </Link>
      </header>

      <section aria-labelledby="active-ticket-heading">
        <h2 id="active-ticket-heading" className="mb-3 text-sm font-semibold text-gray-900">
          내 이용권
        </h2>
        <ActiveTicketCard ticket={activeTicket} />
      </section>

      <section className="mt-8" aria-labelledby="ticket-history-heading">
        <Card>
          <div className="mb-4 flex items-center gap-2">
            <Ticket size={18} className="text-primary" />
            <h2 id="ticket-history-heading" className="font-semibold text-gray-900">
              최근 이용 내역
            </h2>
          </div>
          {historyLoading ? (
            <Loading label="이용 내역을 불러오는 중..." />
          ) : historyError ? (
            <ErrorMessage error={historyError} />
          ) : (
            <TicketHistoryTable items={history?.content ?? []} />
          )}
        </Card>
      </section>
    </div>
  );
};

export default TicketPage;
