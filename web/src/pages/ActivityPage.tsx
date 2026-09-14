import { Link, useParams } from "react-router";
import { useActivity, useGroup } from "../api/queries";
import { ExpressionList } from "../components/ExpressionRow";
import { PageHeader } from "../components/Layout";

/** The group page: my wishes, what I am taking care of, and what nobody has taken yet. */
export function ActivityPage() {
  const id = Number(useParams().id);
  const activity = useActivity(id);
  const group = useGroup(id);
  if (activity.isPending) return <p className="muted">Loading…</p>;
  if (activity.isError || !activity.data) return <p className="error">This group is not available.</p>;
  const a = activity.data;
  const closed = group.data?.status === "CLOSED";
  return (
    <>
      <PageHeader
        title={a.groupName}
        parent={{ to: "/", label: "My Groups" }}
        action={
          <span className="header-links">
            <Link to={`/groups/${id}/members`}>Group users ›</Link>
            <Link to={`/groups/${id}/edit`}>{group.data?.myRole === "ADMIN" ? "Edit" : "About"}</Link>
          </span>
        }
      />
      {closed && <p className="notice">This group is closed. Wishes can be read but not changed.</p>}
      <ExpressionList
        title="My expressions"
        items={a.myExpressions}
        empty="You have not expressed a wish here yet."
        action={
          !closed && (
            <Link to={`/groups/${id}/expressions/new`} className="button ghost block">
              + Create expression
            </Link>
          )
        }
      />
      <ExpressionList
        title="My implementations"
        items={a.myImplementations}
        showCreator
        empty="You are not taking care of anything here."
      />
      <ExpressionList title="Not taken yet" items={a.notTaken} showCreator empty="Every wish has someone taking care of it." />
    </>
  );
}
