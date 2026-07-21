import RequireAuth from "@/components/auth/RequireAuth";

const MyChallengesLayout = ({ children }) => <RequireAuth>{children}</RequireAuth>;

export default MyChallengesLayout;
