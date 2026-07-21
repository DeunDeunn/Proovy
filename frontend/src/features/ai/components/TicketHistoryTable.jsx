import Link from "next/link";

import Badge from "@/components/ui/Badge";

import { AI_TICKET_HISTORY_META, formatAiDateTime } from "../format";

const TicketHistoryTable = ({ items }) => {
  if (!items?.length) {
    return <p className="py-10 text-center text-sm text-gray-500">아직 AI 티켓 이용 내역이 없어요.</p>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[560px] text-sm">
        <thead>
          <tr className="border-b border-gray-100 text-left text-gray-500">
            <th className="pb-3 font-medium">일시</th>
            <th className="pb-3 font-medium">구분</th>
            <th className="pb-3 font-medium">이용권 번호</th>
            <th className="pb-3 text-right font-medium">연결</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => {
            const meta = AI_TICKET_HISTORY_META[item.type] ?? {
              label: item.type,
              variant: "gray",
            };
            const postId = item.certificationPostId ?? item.verificationPostId;

            return (
              <tr key={item.id} className="border-b border-gray-50 last:border-0">
                <td className="py-3 text-gray-600">{formatAiDateTime(item.createdAt)}</td>
                <td className="py-3">
                  <Badge variant={meta.variant}>{meta.label}</Badge>
                </td>
                <td className="py-3 text-gray-600">#{item.subscriptionId}</td>
                <td className="py-3 text-right">
                  {postId ? (
                    <Link
                      href={`/certification-posts/${postId}`}
                      className="font-medium text-primary hover:underline"
                    >
                      인증글 보기
                    </Link>
                  ) : (
                    <span className="text-gray-300">-</span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default TicketHistoryTable;
