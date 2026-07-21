import { CalendarClock, CheckCircle2, Sparkles } from "lucide-react";

import Card from "@/components/ui/Card";

import { formatAiDateTime, getRemainingText } from "../format";

const ActiveTicketCard = ({ ticket }) => {
  if (!ticket?.hasActiveTicket) {
    return (
      <Card className="border-dashed bg-gray-50">
        <div className="flex items-center gap-4">
          <span className="flex h-11 w-11 items-center justify-center rounded-full bg-gray-200 text-gray-500">
            <Sparkles size={20} />
          </span>
          <div>
            <h2 className="font-semibold text-gray-900">사용 중인 AI 티켓이 없어요</h2>
            <p className="mt-1 text-sm text-gray-500">
              이용권을 구매하면 운영 중인 챌린지에서 AI 검수를 사용할 수 있어요.
            </p>
          </div>
        </div>
      </Card>
    );
  }

  return (
    <Card className="border-blue-200 bg-gradient-to-br from-blue-50 to-white">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-4">
          <span className="flex h-11 w-11 items-center justify-center rounded-full bg-primary text-white">
            <CheckCircle2 size={21} />
          </span>
          <div>
            <p className="text-xs font-semibold text-primary">현재 이용권</p>
            <h2 className="mt-1 text-lg font-bold text-gray-900">{ticket.planName}</h2>
            <p className="mt-2 flex items-center gap-1.5 text-sm text-gray-600">
              <CalendarClock size={15} />
              {formatAiDateTime(ticket.startedAt)} ~ {formatAiDateTime(ticket.expiredAt)}
            </p>
          </div>
        </div>
        <span className="rounded-full bg-primary px-3 py-1 text-xs font-bold text-white">
          {getRemainingText(ticket.expiredAt)}
        </span>
      </div>
    </Card>
  );
};

export default ActiveTicketCard;
