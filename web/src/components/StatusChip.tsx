import type { ExpressionStatus, GroupStatus } from "../api/types";

export const EXPRESSION_LABELS: Record<ExpressionStatus, string> = {
  EXPRESSED: "Expressed",
  IN_PROCESS: "In Process",
  PROVIDED: "Provided",
  IN_POSSESSION: "In Possession",
};

const GROUP_LABELS: Record<GroupStatus, string> = {
  NEW: "New",
  WORKING: "Working",
  CLOSED: "Closed",
};

export function StatusChip({ status }: { status: ExpressionStatus }) {
  return <span className={`chip chip-${status.toLowerCase()}`}>{EXPRESSION_LABELS[status]}</span>;
}

export function GroupStatusChip({ status }: { status: GroupStatus }) {
  return <span className={`chip chip-group-${status.toLowerCase()}`}>{GROUP_LABELS[status]}</span>;
}
