import FollowListPage from "@/features/users/pages/FollowListPage";

const Page = async ({ params }) => {
  const { userId } = await params;
  return <FollowListPage userId={userId} type="following" />;
};

export default Page;
