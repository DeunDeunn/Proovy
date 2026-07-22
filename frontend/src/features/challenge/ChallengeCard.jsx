/* eslint-disable @next/next/no-img-element -- S3 썸네일 URL은 현재 next/image 설정 대상이 아니다. */

import Link from "next/link";

import Badge from "@/components/ui/Badge";
import { formatChallengePeriod } from "@/lib/date";
import { statusBadgeMap } from "./categoryVisuals";

const ChallengeCard = ({ challenge, showPendingCertificationBadge = false }) => {
  const statusBadge = statusBadgeMap[challenge.status] ?? statusBadgeMap.RECRUITING;
  const pendingCertificationCount = challenge.pendingCertificationCount ?? 0;

  return (
    <Link
      href={`/challenges/${challenge.id}`}
      className="block overflow-hidden rounded-xl border border-gray-200 bg-white transition-shadow hover:shadow-md"
    >
      <div className="relative h-44 bg-gray-50">
        {challenge.thumbnailUrl ? (
          <img src={challenge.thumbnailUrl} alt="" className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <img src="/logo.png" alt="" className="h-9 w-auto opacity-30" />
          </div>
        )}
        <span
          className={`absolute left-3 top-3 rounded-full px-2 py-1 text-[11px] font-bold text-white ${statusBadge.className}`}
        >
          {statusBadge.label}
        </span>
      </div>
      <div className="space-y-1.5 p-3">
        <div className="flex items-center gap-1.5">
          <h3 className="flex-1 truncate text-base font-bold text-gray-900">{challenge.title}</h3>
          <span className="shrink-0 rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-medium text-gray-500">
            {challenge.categoryName}
          </span>
        </div>
        <div className="flex items-end justify-between gap-3">
          <p className="text-sm font-semibold text-gray-800">
            ₩ {(challenge.entryFee ?? 0).toLocaleString()}
          </p>
          <div className="flex flex-col items-end gap-1">
            <p className="text-xs text-gray-500">
              {challenge.currentParticipants} / {challenge.maxParticipants}명
            </p>
            {showPendingCertificationBadge && pendingCertificationCount > 0 && (
              <Badge variant="danger">미검수 {pendingCertificationCount}건</Badge>
            )}
          </div>
        </div>
        <p className="text-xs text-gray-500">
          {formatChallengePeriod(challenge.startDate, challenge.endDate)}
        </p>
      </div>
    </Link>
  );
};

export default ChallengeCard;
