import Link from "next/link";
import Image from "next/image";
import {
  ArrowRight,
  BookOpen,
  CheckCircle2,
  CircleDollarSign,
  Droplets,
  Flame,
  Gift,
  Heart,
  MessageCircle,
  PenLine,
  ScanSearch,
  ShieldCheck,
  UsersRound,
} from "lucide-react";

const challenges = [
  {
    title: "매일 10분 독서하기",
    period: "05.20 ~ 06.19 (30일)",
    fee: "5,000P",
    successRate: "92%",
    participants: "1,248",
    visual: "book",
  },
  {
    title: "하루 8잔 물 마시기",
    period: "05.18 ~ 06.16 (30일)",
    fee: "3,000P",
    successRate: "89%",
    participants: "967",
    visual: "water",
  },
  {
    title: "매일 감사일기 쓰기",
    period: "05.22 ~ 06.21 (31일)",
    fee: "5,000P",
    successRate: "94%",
    participants: "1,102",
    visual: "journal",
  },
  {
    title: "하루 1% 정리습관",
    period: "05.19 ~ 06.17 (30일)",
    fee: "3,000P",
    successRate: "86%",
    participants: "843",
    visual: "desk",
  },
];

const popularFeeds = [
  {
    name: "미라클모닝",
    time: "2시간 전",
    text: "오늘도 아침 6시에 일어나서 독서 완료! 작은 습관이 쌓여 큰 변화를 만드네요.",
    likes: 128,
    comments: 32,
    avatar: "bg-rose-200",
  },
  {
    name: "꾸준한하루",
    time: "4시간 전",
    text: "물 8잔 마시기 12일차 💧 몸도 마음도 가벼워지는 느낌이에요!",
    likes: 96,
    comments: 18,
    avatar: "bg-amber-200",
  },
  {
    name: "기록하는사람",
    time: "6시간 전",
    text: "감사일기 덕분에 하루가 더 따뜻해져요. 오늘도 감사한 일 가득 채워봐요 :)",
    likes: 74,
    comments: 12,
    avatar: "bg-violet-200",
  },
];

const ChallengeVisual = ({ type }) => {
  const common = "relative flex h-28 items-center justify-center overflow-hidden";

  if (type === "water") {
    return (
      <div className={`${common} bg-gradient-to-br from-slate-100 via-white to-blue-100`}>
        <Droplets className="absolute left-8 top-6 text-blue-300" size={28} />
        <div className="relative h-21 w-14 rounded-b-2xl rounded-t-md border-4 border-white/80 bg-gradient-to-b from-white/70 to-blue-300/90 shadow-lg">
          <div className="absolute inset-x-1 bottom-1 h-10 rounded-b-xl bg-blue-400/70" />
        </div>
      </div>
    );
  }

  if (type === "journal") {
    return (
      <div className={`${common} bg-gradient-to-br from-amber-100 via-orange-50 to-stone-200`}>
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
      <div className={`${common} bg-gradient-to-br from-emerald-100 via-stone-50 to-sky-100`}>
        <div className="absolute bottom-2 h-12 w-44 rounded-t-2xl bg-amber-200/80" />
        <div className="absolute bottom-8 right-11 h-13 w-18 rounded-t-md bg-slate-300 shadow-md" />
        <div className="absolute left-11 top-4 h-16 w-11 rounded-t-full bg-emerald-300/70" />
        <div className="absolute left-8 top-2 h-8 w-17 rounded-full bg-emerald-400/70" />
      </div>
    );
  }

  return (
    <div className={`${common} bg-gradient-to-br from-amber-100 via-stone-50 to-slate-200`}>
      <div className="h-17 w-26 -rotate-6 rounded-sm bg-white p-3 shadow-lg">
        <div className="h-1 w-full bg-stone-300" />
        <div className="mt-3 h-1 w-4/5 bg-stone-200" />
        <div className="mt-2 h-1 w-3/5 bg-stone-200" />
      </div>
      <BookOpen className="absolute bottom-4 right-8 text-amber-700" size={34} />
    </div>
  );
};

const StatRow = ({ icon: Icon, iconClassName, label, value }) => (
  <div className="flex items-center gap-3 border-b border-gray-100 py-3 last:border-b-0">
    <span className={`flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 ${iconClassName}`}>
      <Icon size={19} strokeWidth={2.5} />
    </span>
    <span className="text-sm font-medium text-gray-700">{label}</span>
    <strong className="ml-auto text-sm text-gray-900">{value}</strong>
  </div>
);

const HomePage = () => {
  return (
    <div className="mx-auto max-w-[1440px] space-y-4 pb-2">
      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.9fr)_minmax(300px,0.85fr)]">
        <div className="relative overflow-hidden rounded-3xl">
          <Image
            src="/home-hero-banner.png"
            alt="오늘의 인증이 내일의 보상이 됩니다"
            width={1916}
            height={821}
            priority
            sizes="(min-width: 1536px) 950px, (min-width: 1280px) 62vw, 100vw"
            className="h-auto w-full"
          />
          <Link
            href="/challenges"
            className="absolute bottom-[7%] left-[7%] inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-bold text-white shadow-sm transition-colors hover:bg-primary-hover sm:px-4 sm:py-2"
          >
            챌린지 둘러보기 <ArrowRight size={14} />
          </Link>
        </div>

        <aside className="rounded-3xl border border-gray-200 bg-surface p-5">
          <h2 className="text-base font-bold text-gray-900">오늘의 인증 현황</h2>
          <div className="mt-2">
            <StatRow icon={CheckCircle2} iconClassName="bg-blue-50 text-primary" label="오늘 인증" value="1/1" />
            <StatRow icon={Flame} iconClassName="bg-orange-50 text-orange-500" label="연속 성공일" value="12일" />
            <StatRow icon={CircleDollarSign} iconClassName="bg-blue-50 text-primary" label="보유 포인트" value="12,450P" />
          </div>
          <Link href="/wallet/charge" className="mt-4 flex items-center justify-center rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-white transition-colors hover:bg-primary-hover">
            포인트 충전하기
          </Link>
        </aside>
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.9fr)_minmax(300px,0.85fr)]">
        <div className="rounded-2xl border border-gray-200 bg-surface p-4">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-base font-bold text-gray-900">추천 챌린지</h2>
            <Link href="/challenges" className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-primary">
              더보기 <ArrowRight size={15} />
            </Link>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {challenges.map((challenge) => (
              <article key={challenge.title} className="overflow-hidden rounded-xl border border-gray-200 bg-white">
                <div className="relative">
                  <ChallengeVisual type={challenge.visual} />
                  <span className="absolute left-3 top-3 rounded-full bg-primary px-2 py-1 text-[11px] font-bold text-white">모집중</span>
                </div>
                <div className="p-3">
                  <h3 className="truncate text-sm font-bold text-gray-900">{challenge.title}</h3>
                  <p className="mt-1.5 text-xs text-gray-500">{challenge.period}</p>
                  <div className="mt-3 grid grid-cols-3 gap-2 border-t border-gray-100 pt-2 text-center">
                    <div><strong className="block text-xs text-gray-800">{challenge.fee}</strong><span className="text-[11px] text-gray-400">참가비</span></div>
                    <div><strong className="block text-xs text-gray-800">{challenge.successRate}</strong><span className="text-[11px] text-gray-400">성공률</span></div>
                    <div><strong className="block text-xs text-gray-800">{challenge.participants}</strong><span className="text-[11px] text-gray-400">참가자</span></div>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </div>

        <section className="rounded-2xl border border-gray-200 bg-surface p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <h2 className="text-base font-bold text-gray-900">인기 피드</h2>
            <Link href="/feeds" className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-primary">
              더보기 <ArrowRight size={15} />
            </Link>
          </div>
          <div className="divide-y divide-gray-100">
            {popularFeeds.map((feed) => (
              <article key={feed.name} className="flex gap-3 py-2.5 first:pt-0 last:pb-0">
                <div className={`mt-0.5 h-9 w-9 shrink-0 rounded-full ${feed.avatar}`} />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-bold text-gray-800">{feed.name}<span className="ml-2 text-xs font-normal text-gray-400">· {feed.time}</span></p>
                  <p className="mt-1 line-clamp-2 text-xs leading-5 text-gray-600">{feed.text}</p>
                  <div className="mt-2 flex items-center gap-4 text-xs text-gray-400">
                    <span className="flex items-center gap-1"><Heart size={14} /> {feed.likes}</span>
                    <span className="flex items-center gap-1"><MessageCircle size={14} /> {feed.comments}</span>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>
      </section>

      <section className="grid gap-4 rounded-2xl border border-gray-200 bg-surface p-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          [ShieldCheck, "검증된 방장", "엄격한 기준으로 검증된 방장이 챌린지를 운영해요."],
          [ScanSearch, "AI 인증 검수", "AI가 인증을 꼼꼼하게 검수하여 공정한 참여를 보장해요."],
          [Gift, "성공 시 리워드", "목표 달성 시 참가비와 리워드를 100% 돌려드려요."],
          [UsersRound, "함께 성장하는 커뮤니티", "서로 응원하고 동기부여 받으며 함께 성장해요."],
        ].map(([Icon, title, description]) => (
          <div key={title} className="flex gap-3 xl:not-last:border-r xl:not-last:border-gray-200 xl:pr-4">
            <Icon className="shrink-0 text-primary" size={38} strokeWidth={1.8} />
            <div>
              <h2 className="text-sm font-bold text-gray-900">{title}</h2>
              <p className="mt-1 text-xs leading-5 text-gray-500">{description}</p>
            </div>
          </div>
        ))}
      </section>
    </div>
  );
};

export default HomePage;
