import { Suspense } from "react";

import WithdrawPage from "@/features/mypage/pages/WithdrawPage";

const Page = () => (
  <Suspense fallback={null}>
    <WithdrawPage />
  </Suspense>
);

export default Page;
