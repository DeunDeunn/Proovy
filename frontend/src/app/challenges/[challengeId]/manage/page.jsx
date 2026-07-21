import ChallengeManagePage from "@/features/challenge/ChallengeManagePage";

const Page = async ({ params }) => {
  const { challengeId } = await params;
  return <ChallengeManagePage challengeId={challengeId} />;
};

export default Page;
