import RequireAuth from "@/components/auth/RequireAuth";

const UsersLayout = ({ children }) => <RequireAuth>{children}</RequireAuth>;

export default UsersLayout;
