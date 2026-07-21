import { BookOpen, Droplets, PenLine } from "lucide-react";

import { formatChallengePeriod } from "@/lib/date";

const categoryVisualMap = {
  운동: "water",
  루틴: "journal",
  식습관: "book",
  취미: "desk",
  기타: "book",
};

const ChallengeVisual = ({ type }) => {
  const common = "relative flex h-28 items-center justify-center overflow-hidden";

  if (type === "water") {
    return (
      <div
        aria-hidden="true"
        className={`${common} bg-gradient-to-br from-slate-100 via-white to-blue-100`}
      >
        <Droplets className="absolute left-8 top-6 text-blue-300" size={28} />
        <div className="relative h-21 w-14 rounded-b-2xl rounded-t-md border-4 border-white/80 bg-gradient-to-b from-white/70 to-blue-300/90 shadow-lg">
          <div className="absolute inset-x-1 bottom-1 h-10 rounded-b-xl bg-blue-400/70" />
        </div>
      </div>
    );
  }

  if (type === "journal") {
    return (
      <div
        aria-hidden="true"
        className={`${common} bg-gradient-to-br from-amber-100 via-orange-50 to-stone-200`}
      >
        <div className="h-20 w-28 -rotate-6 rounded-sm bg-white p-3 shadow-lg">
          <div className="h-1 w-16 bg-amber-200" />
          <div className="mt-3 h-1 w-20 bg-amber-100" />
          <div className="mt-2 h-1 w-12 bg-amber-100" />
        </div>
        <PenLine className="absolute right-8 top-7 -rotate-45 text-amber-700" size={34} />
      </div>
    );
  }

  if (type === "desk") {
    return (
      <div
        aria-hidden="true"
        className={`${common} bg-gradient-to-br from-emerald-100 via-stone-50 to-sky-100`}
      >
        <div className="absolute bottom-2 h-12 w-44 rounded-t-2xl bg-amber-200/80" />
        <div className="absolute bottom-8 right-11 h-13 w-18 rounded-t-md bg-slate-300 shadow-md" />
        <div className="absolute left-11 top-4 h-16 w-11 rounded-t-full bg-emerald-300/70" />
        <div className="absolute left-8 top-2 h-8 w-17 rounded-full bg-emerald-400/70" />
      </div>
    );
  }

  return (
    <div
      aria-hidden="true"
      className={`${common} bg-gradient-to-br from-amber-100 via-stone-50 to-slate-200`}
    >
      <div className="h-17 w-26 -rotate-6 rounded-sm bg-white p-3 shadow-lg">
        <div className="h-1 w-full bg-stone-300" />
        <div className="mt-3 h-1 w-4/5 bg-stone-200" />
        <div className="mt-2 h-1 w-3/5 bg-stone-200" />
      </div>
      <BookOpen className="absolute bottom-4 right-8 text-amber-700" size={34} />
    </div>
  );
};

const ChallengeCard = ({ challenge }) => (
  <article className="overflow-hidden rounded-xl border border-gray-200 bg-white">
    <div className="relative">
      <ChallengeVisual type={categoryVisualMap[challenge.categoryName] ?? "book"} />
      <span className="absolute left-3 top-3 rounded-full bg-primary px-2 py-1 text-[11px] font-bold text-white">
        모집중
      </span>
    </div>
    <div className="p-3">
      <h3 className="truncate text-sm font-bold text-gray-900">{challenge.title}</h3>
      <p className="mt-1.5 text-xs text-gray-500">
        {formatChallengePeriod(challenge.startDate, challenge.endDate)}
      </p>
      <div className="mt-3 grid grid-cols-3 gap-2 border-t border-gray-100 pt-2 text-center">
        <div>
          <strong className="block text-xs text-gray-800">
            {(challenge.entryFee ?? 0).toLocaleString()}
          </strong>
          <span className="text-[11px] text-gray-400">참가비</span>
        </div>
        <div>
          <strong className="block text-xs text-gray-800">{challenge.maxParticipants}</strong>
          <span className="text-[11px] text-gray-400">정원</span>
        </div>
        <div>
          <strong className="block text-xs text-gray-800">{challenge.currentParticipants}</strong>
          <span className="text-[11px] text-gray-400">참가자</span>
        </div>
      </div>
    </div>
  </article>
);

export default ChallengeCard;
