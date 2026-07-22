"use client";

import { Bot, CalendarClock, Pencil, Power, ShoppingBag } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { formatAiDateTime, formatAiTicketPlanName } from "@/features/ai/format";
import {
  useActiveAiTicket,
  useAiReviewRule,
  useDeactivateAiReview,
  useUpsertAiReviewRule,
} from "@/features/ai/hooks";
import { useChallenge } from "@/features/challenge/hooks";

import AiReviewRuleDialog from "./AiReviewRuleDialog";

const ChallengeAiSettings = ({ challengeId }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const { data: challenge, isLoading: challengeLoading, error: challengeError } =
    useChallenge(challengeId);
  const { data: activeTicket, isLoading: ticketLoading, error: ticketError } =
    useActiveAiTicket();
  const enabled = challenge?.aiReviewEnabled === true;
  const {
    data: rule,
    isLoading: ruleLoading,
    error: ruleError,
  } = useAiReviewRule(challengeId, { enabled });
  const upsertMutation = useUpsertAiReviewRule(challengeId);
  const deactivateMutation = useDeactivateAiReview(challengeId);

  const closeDialog = () => {
    if (!upsertMutation.isPending) {
      setDialogOpen(false);
      upsertMutation.reset();
    }
  };

  const saveRule = (payload) => {
    upsertMutation.mutate(payload, { onSuccess: () => setDialogOpen(false) });
  };

  const deactivate = () => {
    if (window.confirm("AI 검수를 비활성화할까요? 저장된 기준은 다시 활성화할 때 수정할 수 있어요.")) {
      deactivateMutation.mutate();
    }
  };

  if (challengeLoading || ticketLoading || (enabled && ruleLoading)) {
    return <Loading label="AI 검수 설정을 불러오는 중..." />;
  }
  if (challengeError || ticketError) {
    return <ErrorMessage error={challengeError || ticketError} />;
  }

  const hasActiveTicket = activeTicket?.hasActiveTicket === true;

  return (
    <>
      <Card>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-light text-primary">
              <Bot size={20} />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-gray-900">AI 자동검수</h2>
                <span
                  className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                    enabled ? "bg-green-50 text-green-700" : "bg-gray-100 text-gray-500"
                  }`}
                >
                  {enabled ? "사용 중" : "미사용"}
                </span>
              </div>
              <p className="mt-1 text-sm text-gray-500">
                챌린지 인증글을 설정한 기준에 따라 AI가 검수해요.
              </p>
            </div>
          </div>

          {enabled ? (
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => setDialogOpen(true)}>
                <span className="inline-flex items-center gap-1.5">
                  <Pencil size={15} /> 기준 수정
                </span>
              </Button>
              <Button
                variant="outline"
                onClick={deactivate}
                disabled={deactivateMutation.isPending}
              >
                <span className="inline-flex items-center gap-1.5">
                  <Power size={15} />
                  {deactivateMutation.isPending ? "처리 중..." : "비활성화"}
                </span>
              </Button>
            </div>
          ) : hasActiveTicket ? (
            <Button onClick={() => setDialogOpen(true)}>AI 검수 활성화</Button>
          ) : (
            <Link
              href="/mypage/tickets/store"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-hover"
            >
              <ShoppingBag size={16} /> 티켓 구매하기
            </Link>
          )}
        </div>

        <div className="mt-5 border-t border-gray-100 pt-5">
          {enabled && rule ? (
            <div className="space-y-4">
              <div>
                <p className="text-xs font-medium text-gray-400">검수 방식</p>
                <p className="mt-1 text-sm font-semibold text-gray-800">
                  {rule.reviewMode === "AUTO" ? "자동 검수" : "검수 보조"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-400">현재 검수 기준</p>
                <p className="mt-1 whitespace-pre-line text-sm leading-6 text-gray-700">
                  {rule.ruleText}
                </p>
              </div>
            </div>
          ) : enabled && ruleError ? (
            <div className="space-y-3">
              <ErrorMessage error={ruleError} />
              <Button variant="outline" onClick={() => setDialogOpen(true)}>
                검수 기준 다시 설정
              </Button>
            </div>
          ) : hasActiveTicket ? (
            <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
              <span className="font-medium text-gray-700">
                {formatAiTicketPlanName(activeTicket.planName)} 사용 중
              </span>
              <span className="inline-flex items-center gap-1.5 text-gray-500">
                <CalendarClock size={15} /> {formatAiDateTime(activeTicket.expiredAt)} 만료
              </span>
            </div>
          ) : (
            <p className="text-sm text-gray-500">
              AI 검수를 활성화하려면 먼저 사용할 티켓을 구매해주세요.
            </p>
          )}
        </div>

        {deactivateMutation.error && (
          <div className="mt-4">
            <ErrorMessage error={deactivateMutation.error} />
          </div>
        )}
      </Card>

      {dialogOpen && (
        <AiReviewRuleDialog
          initialRule={rule}
          isPending={upsertMutation.isPending}
          error={upsertMutation.error}
          onClose={closeDialog}
          onSubmit={saveRule}
        />
      )}
    </>
  );
};

export default ChallengeAiSettings;
