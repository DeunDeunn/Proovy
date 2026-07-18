import CertificationPostEditPage from "@/features/certification/CertificationPostEditPage";

const Page = async ({ params }) => {
  const { postId } = await params;
  return <CertificationPostEditPage postId={postId} />;
};

export default Page;
