import RequireAuth from "@/components/auth/RequireAuth";

const WalletLayout = ({ children }) => <RequireAuth>{children}</RequireAuth>;

export default WalletLayout;
