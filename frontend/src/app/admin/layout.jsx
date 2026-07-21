import RequireAdmin from "@/components/auth/RequireAdmin";

const AdminLayout = ({ children }) => <RequireAdmin>{children}</RequireAdmin>;

export default AdminLayout;
