import { Link } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useInbox } from "../api/queries";
import type { NotificationView } from "../api/types";
import { PageHeader } from "../components/Layout";
import { formatTime } from "../components/format";

/** What the bell opens: the latest notifications, newest first, each linking to its page. */
export function NotificationsPage() {
  const inbox = useInbox();
  const queryClient = useQueryClient();
  const readAll = useMutation({
    mutationFn: () => api<void>("/api/notifications/read-all", "POST"),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });
  const readOne = useMutation({
    mutationFn: (id: number) => api<void>(`/api/notifications/${id}/read`, "POST"),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });
  return (
    <>
      <PageHeader
        title="Notifications"
        parent={{ to: "/", label: "My Groups" }}
        action={
          inbox.data && inbox.data.unread > 0 ? (
            <button className="link" onClick={() => readAll.mutate()}>
              Mark all read
            </button>
          ) : undefined
        }
      />
      {inbox.isPending && <p className="muted">Loading…</p>}
      {inbox.data && inbox.data.items.length === 0 && <p className="muted">Nothing yet.</p>}
      {inbox.data && (
        <ul className="list">
          {inbox.data.items.map((n) => (
            <li key={n.id} className={n.read ? "list-item" : "list-item unread"}>
              <Link to={target(n)} onClick={() => !n.read && readOne.mutate(n.id)} className="notification">
                <span>{n.message}</span>
                <span className="muted small">{formatTime(n.createdAt)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

function target(n: NotificationView) {
  if (n.type === "CONTACT_MESSAGE") return "/admin";
  if (n.expressionId) return `/expressions/${n.expressionId}`;
  if (n.groupId) return `/groups/${n.groupId}`;
  return "/";
}
