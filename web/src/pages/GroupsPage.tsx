import { Link } from "react-router";
import { useGroups } from "../api/queries";
import { PageHeader } from "../components/Layout";
import { GroupStatusChip } from "../components/StatusChip";

/** The landing page: every group the member belongs to, with the two attention marks. */
export function GroupsPage() {
  const groups = useGroups();
  return (
    <>
      <PageHeader title="My Groups" />
      {groups.isPending && <p className="muted">Loading…</p>}
      {groups.data && groups.data.length === 0 && (
        <p className="muted">You are not in any group yet. Create one, or ask for an invitation.</p>
      )}
      {groups.data && groups.data.length > 0 && (
        <div className="rows">
          {groups.data.map((g) => (
            <Link key={g.id} to={`/groups/${g.id}`} className="row">
              <div className="row-body">
                <div className="row-title">{g.name}</div>
                <div className="row-meta">Owner: {g.ownerName}</div>
                <GroupStatusChip status={g.status} />
              </div>
              <div className="row-side marks">
                {g.hasUntaken && (
                  <span className="bang" title="Wishes nobody has taken care of yet" aria-label="Untaken wishes">
                    !
                  </span>
                )}
                {g.implementing && (
                  <span className="warn" title="You are taking care of something here" aria-label="You are implementing">
                    ▲
                  </span>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
      <Link to="/groups/new" className="button primary block">
        Create group
      </Link>
    </>
  );
}
