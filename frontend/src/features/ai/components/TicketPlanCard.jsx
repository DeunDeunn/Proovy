import { Check, Clock3 } from "lucide-react";

import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";

import { formatAiTicketPlanName, formatAiTicketPrice } from "../format";

const TicketPlanCard = ({ plan, disabled, isPurchasing, onPurchase }) => (
  <Card className="flex h-full flex-col transition-shadow hover:shadow-sm">
    <div className="flex items-center justify-between">
      <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-light text-primary">
        <Clock3 size={18} />
      </span>
      <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-semibold text-gray-600">
        {plan.durationDays}일
      </span>
    </div>
    <h3 className="mt-4 text-lg font-bold text-gray-900">
      {formatAiTicketPlanName(plan.name)}
    </h3>
    <p className="mt-2 min-h-10 text-sm leading-5 text-gray-500">
      {plan.description || `${plan.durationDays}일 동안 AI 검수 기능을 사용할 수 있어요.`}
    </p>
    <ul className="mt-4 space-y-2 text-sm text-gray-600">
      <li className="flex items-center gap-2">
        <Check size={15} className="text-success" />
        AI 인증글 검수
      </li>
      <li className="flex items-center gap-2">
        <Check size={15} className="text-success" />
        자동 승인 또는 검수 보조
      </li>
    </ul>
    <div className="mt-auto pt-6">
      <p className="mb-3 text-xl font-bold text-gray-900">{formatAiTicketPrice(plan.price)}</p>
      <Button className="w-full" disabled={disabled || isPurchasing} onClick={() => onPurchase(plan)}>
        {disabled ? "이용권 사용 중" : isPurchasing ? "구매 처리 중..." : "구매하기"}
      </Button>
    </div>
  </Card>
);

export default TicketPlanCard;
