"use client";

import Card from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";

import { useSettlementResult, useTransactions } from "../hooks";
import { useChallenge } from "@/features/challenge/hooks";
import { formatCurrency } from "../format";

const PERCENT = (value) => `${Number(value ?? 0)}%`;

const SettlementResultPage = ({ challengeId }) => {
  const {
    data: settlement,
    isLoading: settlementLoading,
    error: settlementError,
  } = useSettlementResult(challengeId);
  const { data: challenge, isLoading: challengeLoading } = useChallenge(challengeId);

  // 정산으로 인해 내 지갑에 반영된 참가비 관련 거래를 찾아 "내 결과"를 판단
  // (SettlementResultResponse는 참가자 전체 집계만 담고 있어 개인 성공/실패 여부가 없음)
  const {
    data: successTx,
    isLoading: successTxLoading,
    error: successTxError,
  } = useTransactions({ type: "CHALLENGE_PRINCIPAL_SUCCESS" });
  const {
    data: failTx,
    isLoading: failTxLoading,
    error: failTxError,
  } = useTransactions({ type: "CHALLENGE_PRINCIPAL_FAIL" });

  const myResultLoading = successTxLoading || failTxLoading;
  const myResultError = successTxError || failTxError;

  if (settlementLoading || challengeLoading) return <Loading label="정산 결과를 불러오는 중..." />;
  if (settlementError) return <ErrorMessage error={settlementError} />;
  if (!settlement) return null;

  const myResult = [...(successTx?.content ?? []), ...(failTx?.content ?? [])].find(
    (item) => String(item.referenceId) === String(challengeId)
  );
  const isSuccess = myResult?.type === "CHALLENGE_PRINCIPAL_SUCCESS";
  const hasResult = !!myResult;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900">정산 결과</h1>
        <p className="mt-1 text-sm text-gray-500">챌린지 종료 후 최종 정산 내역을 확인하세요.</p>
      </div>

      <div className="grid grid-cols-3 gap-6">
        <Card className="col-span-2">
          <div className="mb-4 flex items-center gap-4">
            <div className="h-20 w-28 shrink-0 rounded-lg bg-gray-100" />
            <div>
              <h2 className="font-semibold text-gray-900">{challenge?.title ?? `챌린지 #${challengeId}`}</h2>
              {challenge?.startDate && challenge?.endDate && (
                <p className="mt-1 text-sm text-gray-500">
                  {challenge.startDate} ~ {challenge.endDate}
                </p>
              )}
              {challenge?.entryFee != null && (
                <p className="mt-1 text-xs text-gray-400">
                  참가비 {formatCurrency(challenge.entryFee)} · 참가자{" "}
                  {challenge.currentParticipants ?? settlement.totalParticipantCount}명 · 성공 기준{" "}
                  {challenge.successCriteriaRate ?? "-"}%
                </p>
              )}
            </div>
          </div>

          {myResultLoading ? (
            <div className="rounded-lg border border-gray-100 p-4">
              <Loading label="내 참여 결과를 확인하는 중..." />
            </div>
          ) : myResultError ? (
            <div className="rounded-lg border border-gray-100 p-4">
              <ErrorMessage error={myResultError} />
            </div>
          ) : hasResult ? (
            <div className="rounded-lg border border-gray-100 p-4">
              <div className="mb-2 flex items-center gap-2">
                <span className="text-sm text-gray-500">내 결과</span>
                <Badge variant={isSuccess ? "success" : "danger"}>
                  {isSuccess ? "성공" : "실패"}
                </Badge>
              </div>
              <p className="text-sm text-gray-600">
                {isSuccess ? "축하해요! 챌린지에 성공했어요." : "아쉽지만 이번엔 성공하지 못했어요."}
              </p>
            </div>
          ) : (
            <div className="rounded-lg border border-gray-100 p-4 text-sm text-gray-500">
              내 참여 결과를 아직 확인할 수 없어요.
            </div>
          )}

          <div className="my-4 border-t border-gray-100" />

          {myResultLoading || myResultError ? (
            <p className="text-sm text-gray-500">내 정산 금액을 확인하는 중이에요.</p>
          ) : (
            <>
              <h3 className="mb-3 text-sm font-semibold text-gray-700">최종 정산 내역</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">참가비 반환</span>
                  <span className="font-medium text-gray-900">
                    +{formatCurrency(challenge?.entryFee)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">정산 수익</span>
                  <span className="font-medium text-gray-900">
                    +{formatCurrency(settlement.profitPerUser)}
                  </span>
                </div>
              </div>

              <div className="my-4 border-t border-gray-100" />

              <div className="flex items-center justify-between">
                <span className="font-medium text-gray-700">정산 후 획득 캐시</span>
                <span className="text-lg font-bold text-success">
                  +{formatCurrency((challenge?.entryFee ?? 0) + (settlement.profitPerUser ?? 0))}
                </span>
              </div>
            </>
          )}
        </Card>

        <Card>
          <h2 className="mb-4 font-semibold text-gray-900">전체 정산 요약</h2>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">총 참가자</span>
              <span className="font-medium text-gray-900">
                {settlement.totalParticipantCount}명
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">성공</span>
              <span className="font-medium text-success">
                {settlement.successUserCount}명 (
                {settlement.totalParticipantCount
                  ? Math.round((settlement.successUserCount / settlement.totalParticipantCount) * 100)
                  : 0}
                %)
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">실패</span>
              <span className="font-medium text-danger">
                {settlement.failUserCount}명 (
                {settlement.totalParticipantCount
                  ? Math.round((settlement.failUserCount / settlement.totalParticipantCount) * 100)
                  : 0}
                %)
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">실패자 풀</span>
              <span className="font-medium text-gray-900">
                {formatCurrency(settlement.failurePool)}
              </span>
            </div>
          </div>

          <div className="my-4 border-t border-gray-100" />

          <h3 className="mb-2 text-sm font-semibold text-gray-700">분배 비율 및 금액</h3>
          <div className="space-y-2 text-sm">
            <div className="flex items-center justify-between">
              <span className="flex items-center gap-2 text-gray-500">
                <span className="h-2 w-2 rounded-full bg-primary" />
                성공자 분배 {PERCENT(settlement.participantShareRate)}
              </span>
              <span className="text-gray-700">{formatCurrency(settlement.participantShareAmount)}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="flex items-center gap-2 text-gray-500">
                <span className="h-2 w-2 rounded-full bg-purple-400" />
                플랫폼 수수료 {PERCENT(settlement.platformFeeRate)}
              </span>
              <span className="text-gray-700">{formatCurrency(settlement.platformFeeAmount)}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="flex items-center gap-2 text-gray-500">
                <span className="h-2 w-2 rounded-full bg-amber-400" />
                방장 보상 {PERCENT(settlement.hostFeeRate)}
              </span>
              <span className="text-gray-700">{formatCurrency(settlement.hostFeeAmount)}</span>
            </div>
          </div>
        </Card>
      </div>

      <p className="mt-4 text-xs text-gray-400">
        정산 수익은 리워드 캐시로 지급되며, 출금 신청이 가능합니다. 정산 내역은 정산 완료일로부터
        1년간 보관됩니다.
      </p>
    </div>
  );
};

export default SettlementResultPage;
