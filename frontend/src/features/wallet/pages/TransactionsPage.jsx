"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

import Card from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";

import { useTransactions } from "../hooks";
import { formatCurrency, formatDate, formatSignedAmount, getTransactionBadge } from "../format";

const TransactionsPage = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useTransactions({ page });

  const transactions = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900">전체 캐시 내역</h1>
        <p className="mt-1 text-sm text-gray-500">충전, 참가비, 정산 등 모든 캐시 이동 내역이에요.</p>
      </div>

      <Card>
        {isLoading ? (
          <Loading label="거래 내역을 불러오는 중..." />
        ) : error ? (
          <ErrorMessage error={error} />
        ) : transactions.length === 0 ? (
          <p className="py-12 text-center text-sm text-gray-500">아직 캐시 이동 내역이 없어요.</p>
        ) : (
          <>
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
                {transactions.map((item) => {
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

            {totalPages > 1 && (
              <div className="mt-5 flex items-center justify-center gap-3">
                <Button
                  variant="outline"
                  className="px-2 py-1.5"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  <ChevronLeft size={16} />
                </Button>
                <span className="text-sm text-gray-500">
                  {page + 1} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  className="px-2 py-1.5"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  <ChevronRight size={16} />
                </Button>
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  );
};

export default TransactionsPage;
