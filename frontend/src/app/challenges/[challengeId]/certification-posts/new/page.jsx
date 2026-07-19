import CertificationPostCreatePage from "@/features/certification/CertificationPostCreatePage";

const Page = async ({ params }) => {
  const { challengeId } = await params;
  return <CertificationPostCreatePage challengeId={challengeId} />;
};

export default Page;
