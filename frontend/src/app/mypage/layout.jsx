import RequireAuth from "@/components/auth/RequireAuth";

const MyPageLayout = ({ children }) => <RequireAuth>{children}</RequireAuth>;

export default MyPageLayout;
