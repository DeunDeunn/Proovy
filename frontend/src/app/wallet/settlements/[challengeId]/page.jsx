import SettlementResultPage from "@/features/wallet/pages/SettlementResultPage";

const Page = async ({ params }) => {
  const { challengeId } = await params;
  return <SettlementResultPage challengeId={challengeId} />;
};

export default Page;
