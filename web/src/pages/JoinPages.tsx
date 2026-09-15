import { useEffect } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useLogout, useMe } from "../api/queries";
import { ErrorText } from "../components/Form";

/** The page an emailed invitation link opens. Public until the person accepts. */
export function InvitePage() {
  const token = useSearchParams()[0].get("token") ?? "";
  const info = useQuery({
    queryKey: ["invitation", token],
    queryFn: () => api<{ groupName: string; email: string; invitedBy: string }>(`/api/invitations/${token}`),
    enabled: token !== "",
  });
  const accept = useJoin(() => api<{ groupId: number }>(`/api/invitations/${token}/accept`, "POST"));
  const me = useMe();
  const logout = useLogout();
  const here = `/invite?token=${encodeURIComponent(token)}`;

  // Opening the link as the invited person is the acceptance; no extra click needed.
  const invitedMe = !!me.data && !!info.data && me.data.email.toLowerCase() === info.data.email.toLowerCase();
  useEffect(() => {
    if (invitedMe && accept.isIdle) {
      accept.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [invitedMe]);

  if (info.isPending) return <main className="page narrow">Loading…</main>;
  if (info.isError || !info.data) {
    return (
      <main className="page narrow">
        <h1 className="brand-title">Invitation</h1>
        <p className="error">This invitation is invalid or has expired. Ask for a new one.</p>
      </main>
    );
  }
  return (
    <main className="page narrow">
      <h1 className="brand-title">Join {info.data.groupName}</h1>
      <p>
        {info.data.invitedBy} invited <strong>{info.data.email}</strong> to the group “{info.data.groupName}”.
      </p>
      {me.data && me.data.email.toLowerCase() !== info.data.email.toLowerCase() ? (
        <>
          <p>
            You are logged in as <strong>{me.data.name}</strong> ({me.data.email}), and this invitation is for a
            different address. Log out, then open the link again and register or log in as {info.data.email}.
          </p>
          <button className="primary" onClick={() => logout.mutate()} disabled={logout.isPending}>
            Log out
          </button>
        </>
      ) : me.data ? (
        <>
          <ErrorText error={accept.error} />
          {accept.isError ? (
            <button className="primary" onClick={() => accept.mutate()} disabled={accept.isPending}>
              Try again
            </button>
          ) : (
            <p className="muted">Joining as {me.data.name}…</p>
          )}
        </>
      ) : (
        <p className="links">
          <Link to={`/register?email=${encodeURIComponent(info.data.email)}&next=${encodeURIComponent(here)}`} className="button primary block">
            Register and join
          </Link>
          <Link to="/login" state={{ next: here }}>
            I already have an account
          </Link>
        </p>
      )}
    </main>
  );
}

/** The page a share link opens. */
export function JoinPage() {
  const token = useParams().token ?? "";
  const info = useQuery({
    queryKey: ["join", token],
    queryFn: () => api<{ groupName: string }>(`/api/join/${token}`),
  });
  const join = useJoin(() => api<{ groupId: number }>(`/api/join/${token}`, "POST"));
  const me = useMe();
  const here = `/join/${token}`;

  if (info.isPending) return <main className="page narrow">Loading…</main>;
  if (info.isError || !info.data) {
    return (
      <main className="page narrow">
        <h1 className="brand-title">Join a group</h1>
        <p className="error">This link is no longer valid.</p>
      </main>
    );
  }
  return (
    <main className="page narrow">
      <h1 className="brand-title">Join {info.data.groupName}</h1>
      {me.data ? (
        <>
          <ErrorText error={join.error} />
          <button className="primary" onClick={() => join.mutate()} disabled={join.isPending}>
            Join as {me.data.name}
          </button>
        </>
      ) : (
        <p className="links">
          <Link to={`/register?next=${encodeURIComponent(here)}`} className="button primary block">
            Register and join
          </Link>
          <Link to="/login" state={{ next: here }}>
            I already have an account
          </Link>
        </p>
      )}
    </main>
  );
}

function useJoin(call: () => Promise<{ groupId: number }>) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: call,
    onSuccess: (r) => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      navigate(`/groups/${r.groupId}`, { replace: true });
    },
  });
}
