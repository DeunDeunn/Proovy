import ChallengeFeedPage from "@/features/challengeFeed/ChallengeFeedPage";

const Page = async ({ params }) => {
  const { challengeId } = await params;
  return <ChallengeFeedPage challengeId={challengeId} />;
};

export default Page;
