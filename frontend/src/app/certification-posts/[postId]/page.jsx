import CertificationPostDetailPage from "@/features/certification/CertificationPostDetailPage";

const Page = async ({ params }) => {
  const { postId } = await params;
  return <CertificationPostDetailPage postId={postId} />;
};

export default Page;
