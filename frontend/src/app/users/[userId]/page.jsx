import UserProfilePage from "@/features/users/pages/UserProfilePage";

const Page = async ({ params }) => {
  const { userId } = await params;
  return <UserProfilePage userId={userId} />;
};

export default Page;
