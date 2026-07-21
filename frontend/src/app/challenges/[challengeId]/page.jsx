import ChallengeDetailPage from "@/features/challenge/ChallengeDetailPage";

const Page = async ({ params }) => {
  const { challengeId } = await params;
  return <ChallengeDetailPage challengeId={challengeId} />;
};

export default Page;
