import { formatChallengePeriod } from "@/lib/date";

const statusBadgeMap = {
  RECRUITING: { label: "모집중", className: "bg-primary" },
  IN_PROGRESS: { label: "진행중", className: "bg-orange-500" },
  COMPLETED: { label: "종료", className: "bg-gray-400" },
  CANCELLED: { label: "취소됨", className: "bg-gray-400" },
};

// 카테고리 id(운동=1, 루틴=2, 식습관=3, 취미=4, 기타=5) 기준 — 이름 문자열이 바뀌어도 색상이 깨지지 않도록 id로 매칭
const categoryGradientMap = {
  1: "from-blue-100 via-white to-sky-100",
  2: "from-amber-100 via-orange-50 to-stone-100",
  3: "from-emerald-100 via-white to-teal-100",
  4: "from-violet-100 via-white to-purple-100",
  5: "from-slate-100 via-white to-gray-100",
};
const defaultGradient = categoryGradientMap[5];

const ChallengeCard = ({ challenge }) => {
  const statusBadge = statusBadgeMap[challenge.status] ?? statusBadgeMap.RECRUITING;
  const gradient = categoryGradientMap[challenge.categoryId] ?? defaultGradient;

  return (
    <article className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <div className={`relative h-28 bg-gradient-to-br ${gradient}`}>
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
        <div className="flex items-center justify-between">
          <p className="text-sm font-semibold text-gray-800">
            ₩ {(challenge.entryFee ?? 0).toLocaleString()}
          </p>
          <p className="text-xs text-gray-500">
            {challenge.currentParticipants} / {challenge.maxParticipants}명
          </p>
        </div>
        <p className="text-xs text-gray-500">
          {formatChallengePeriod(challenge.startDate, challenge.endDate)}
        </p>
      </div>
    </article>
  );
};

export default ChallengeCard;
