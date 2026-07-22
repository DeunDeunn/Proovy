import MyChallengePage from "@/features/myChallenge/MyChallengePage";

const Page = async ({ searchParams }) => {
  const { tab } = await searchParams;
  return <MyChallengePage initialTab={tab} />;
};

export default Page;
