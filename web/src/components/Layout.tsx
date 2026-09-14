import type { ReactNode } from "react";
import { Link } from "react-router";
import { useUnreadCount } from "../api/queries";
import type { User } from "../api/types";

/**
 * The header on every logged-in page: app name, the bell with its unread count, and the
 * member's name leading to the profile. Pages put their one parent link in the
 * PageHeader below; the browser's back gesture does the rest.
 */
export function Layout({ me, children }: { me: User; children: ReactNode }) {
  const unread = useUnreadCount();
  const count = unread.data?.unread ?? 0;
  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">
          Express &amp; Possess
        </Link>
        <nav className="topbar-actions">
          <Link to="/notifications" className="bell" aria-label={`Notifications, ${count} unread`}>
            <span aria-hidden="true">🔔</span>
            {count > 0 && <span className="badge">{count > 99 ? "99+" : count}</span>}
          </Link>
          {me.role === "ADMIN" && (
            <Link to="/admin" className="me" aria-label="Administration">
              ⚙
            </Link>
          )}
          <Link to="/profile" className="me">
            {me.name}
          </Link>
        </nav>
      </header>
      <main className="page">{children}</main>
    </>
  );
}

/** Title row with the page's parent link on the left. */
export function PageHeader({
  title,
  parent,
  action,
}: {
  title: ReactNode;
  parent?: { to: string; label: string };
  action?: ReactNode;
}) {
  return (
    <div className="page-header">
      {parent && (
        <Link to={parent.to} className="parent">
          ← {parent.label}
        </Link>
      )}
      <div className="page-title-row">
        <h1>{title}</h1>
        {action}
      </div>
    </div>
  );
}
