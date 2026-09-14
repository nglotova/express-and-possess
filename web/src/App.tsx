import { Navigate, Outlet, Route, Routes, useLocation } from "react-router";
import { useMe } from "./api/queries";
import { Layout } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ForgotPasswordPage, ResetPasswordPage } from "./pages/PasswordResetPages";
import { GroupsPage } from "./pages/GroupsPage";
import { GroupEditPage } from "./pages/GroupEditPage";
import { ActivityPage } from "./pages/ActivityPage";
import { NewExpressionPage } from "./pages/NewExpressionPage";
import { ExpressionPage } from "./pages/ExpressionPage";
import { GroupUsersPage } from "./pages/GroupUsersPage";
import { MemberExpressionsPage } from "./pages/MemberExpressionsPage";
import { ProfilePage } from "./pages/ProfilePage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { InvitePage, JoinPage } from "./pages/JoinPages";
import { AdminPage } from "./pages/AdminPage";

/** Pages behind login. Sends a visitor to the login page and back again afterwards. */
function RequireAuth() {
  const me = useMe();
  const location = useLocation();
  if (me.isPending) {
    return <p className="muted center">Loading…</p>;
  }
  if (!me.data) {
    return <Navigate to="/login" state={{ next: location.pathname + location.search }} replace />;
  }
  return (
    <Layout me={me.data}>
      <Outlet />
    </Layout>
  );
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/invite" element={<InvitePage />} />
      <Route path="/join/:token" element={<JoinPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/" element={<GroupsPage />} />
        <Route path="/groups/new" element={<GroupEditPage />} />
        <Route path="/groups/:id" element={<ActivityPage />} />
        <Route path="/groups/:id/edit" element={<GroupEditPage />} />
        <Route path="/groups/:id/members" element={<GroupUsersPage />} />
        <Route path="/groups/:id/members/:userId" element={<MemberExpressionsPage />} />
        <Route path="/groups/:id/expressions/new" element={<NewExpressionPage />} />
        <Route path="/expressions/:id" element={<ExpressionPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
