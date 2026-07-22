import ChallengeManagePage from "@/features/challenge/ChallengeManagePage";

const Page = async ({ params, searchParams }) => {
  const { challengeId } = await params;
  const { tab } = await searchParams;
  return <ChallengeManagePage challengeId={challengeId} initialTab={tab} />;
};

export default Page;
