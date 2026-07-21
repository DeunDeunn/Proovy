import RequireAuth from "@/components/auth/RequireAuth";

const ChallengeManageLayout = ({ children }) => <RequireAuth>{children}</RequireAuth>;

export default ChallengeManageLayout;
