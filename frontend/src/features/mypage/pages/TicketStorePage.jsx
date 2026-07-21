"use client";

import { ShoppingBag } from "lucide-react";
import { useState } from "react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import ActiveTicketCard from "@/features/ai/components/ActiveTicketCard";
import TicketPlanCard from "@/features/ai/components/TicketPlanCard";
import { formatAiTicketPlanName, formatAiTicketPrice } from "@/features/ai/format";
import {
  useActiveAiTicket,
  useAiTicketPlans,
  usePurchaseAiTicket,
} from "@/features/ai/hooks";

const TicketStorePage = () => {
  const [selectedPlan, setSelectedPlan] = useState(null);
  const { data: plans, isLoading: plansLoading, error: plansError } = useAiTicketPlans();
  const { data: activeTicket, isLoading: activeLoading, error: activeError } =
    useActiveAiTicket();
  const purchaseMutation = usePurchaseAiTicket();

  const hasActiveTicket = activeTicket?.hasActiveTicket === true;

  const purchase = () => {
    if (!selectedPlan) return;
    purchaseMutation.mutate(selectedPlan.id, {
      onSuccess: () => setSelectedPlan(null),
    });
  };

  if (plansLoading || activeLoading) return <Loading label="AI 티켓 스토어를 불러오는 중..." />;
  if (plansError || activeError) return <ErrorMessage error={plansError || activeError} />;

  return (
    <div className="mx-auto max-w-6xl">
      <header className="mb-6">
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-light text-primary">
            <ShoppingBag size={19} />
          </span>
          <h1 className="text-xl font-bold text-gray-900">AI 티켓 스토어</h1>
        </div>
        <p className="mt-2 text-sm text-gray-500">
          챌린지에 사용할 티켓을 구매해주세요. 구매시 자동으로 활성화 됩니다.
        </p>
      </header>

      {hasActiveTicket && (
        <section className="mb-8" aria-labelledby="store-active-ticket-heading">
          <h2 id="store-active-ticket-heading" className="mb-3 text-sm font-semibold text-gray-900">
            사용 중인 이용권
          </h2>
          <ActiveTicketCard ticket={activeTicket} />
          <p className="mt-2 text-xs text-gray-500">
            현재 이용권이 만료된 후 새로운 이용권을 구매할 수 있어요.
          </p>
        </section>
      )}

      <section aria-labelledby="store-ticket-plans-heading">
        <h2 id="store-ticket-plans-heading" className="mb-3 text-sm font-semibold text-gray-900">
          이용권 상품
        </h2>
        {!plans?.length ? (
          <Card className="text-center text-sm text-gray-500">현재 판매 중인 이용권이 없어요.</Card>
        ) : (
          <div className="grid gap-4 md:grid-cols-3">
            {plans.map((plan) => (
              <TicketPlanCard
                key={plan.id}
                plan={plan}
                disabled={hasActiveTicket}
                isPurchasing={purchaseMutation.isPending}
                onPurchase={setSelectedPlan}
              />
            ))}
          </div>
        )}
      </section>

      {purchaseMutation.error && (
        <div className="mt-4">
          <ErrorMessage error={purchaseMutation.error} />
        </div>
      )}

      {selectedPlan && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="purchase-dialog-title"
            className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl"
          >
            <h2 id="purchase-dialog-title" className="text-lg font-bold text-gray-900">
              AI 티켓을 구매할까요?
            </h2>
            <p className="mt-2 text-sm leading-6 text-gray-500">
              <strong className="text-gray-800">
                {formatAiTicketPlanName(selectedPlan.name)}
              </strong>
              을 {" "}
              {formatAiTicketPrice(selectedPlan.price)}로 구매합니다. 구매 즉시 이용 기간이 시작돼요.
            </p>
            <div className="mt-6 flex justify-end gap-2">
              <Button
                variant="outline"
                disabled={purchaseMutation.isPending}
                onClick={() => setSelectedPlan(null)}
              >
                취소
              </Button>
              <Button disabled={purchaseMutation.isPending} onClick={purchase}>
                {purchaseMutation.isPending ? "구매 중..." : "구매하기"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TicketStorePage;
