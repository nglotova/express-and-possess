import { useParams } from "react-router";
import { useGroup, useMemberExpressions } from "../api/queries";
import { ExpressionList } from "../components/ExpressionRow";
import { PageHeader } from "../components/Layout";

/** One member's wish list in this group. */
export function MemberExpressionsPage() {
  const params = useParams();
  const groupId = Number(params.id);
  const userId = Number(params.userId);
  const group = useGroup(groupId);
  const list = useMemberExpressions(groupId, userId);
  const member = group.data?.members.find((m) => m.userId === userId);
  if (list.isPending) return <p className="muted">Loading…</p>;
  if (list.isError || !list.data) return <p className="error">This list is not available.</p>;
  return (
    <>
      <PageHeader title={member?.name ?? "Member"} parent={{ to: `/groups/${groupId}/members`, label: "Group users" }} />
      <ExpressionList title="Wishes" items={list.data} empty="No wishes in this group." />
    </>
  );
}
