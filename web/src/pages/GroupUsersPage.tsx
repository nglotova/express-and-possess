import { Link, useParams } from "react-router";
import { useGroup } from "../api/queries";
import { PageHeader } from "../components/Layout";

/** Everyone in the group; a row opens that member's wish list. */
export function GroupUsersPage() {
  const id = Number(useParams().id);
  const group = useGroup(id);
  if (group.isPending) return <p className="muted">Loading…</p>;
  if (group.isError || !group.data) return <p className="error">This group is not available.</p>;
  return (
    <>
      <PageHeader title="Group users" parent={{ to: `/groups/${id}`, label: group.data.name }} />
      <div className="rows">
        {group.data.members.map((m) => (
          <Link key={m.userId} to={`/groups/${id}/members/${m.userId}`} className="row">
            <div className="row-body">
              <div className="row-title">{m.name}</div>
              <div className="row-meta">{m.email}</div>
            </div>
            <div className="row-side">
              {m.hasExpressions ? <span className="muted">has wishes</span> : <span className="muted">no wishes yet</span>}
            </div>
          </Link>
        ))}
      </div>
    </>
  );
}
