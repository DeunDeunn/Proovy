import { Suspense } from "react";
import ChargeReturnPage from "@/features/wallet/pages/ChargeReturnPage";

const Page = () => (
  <Suspense fallback={null}>
    <ChargeReturnPage />
  </Suspense>
);

export default Page;
