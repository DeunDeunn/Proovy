import { Suspense } from "react";

import CallbackPage from "@/features/auth/CallbackPage";

const Page = () => (
  <Suspense fallback={null}>
    <CallbackPage />
  </Suspense>
);

export default Page;
