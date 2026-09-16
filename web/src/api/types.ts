// Mirrors the API's response records. Keep field names identical to the Java side.

export type Role = "MEMBER" | "ADMIN";

export interface User {
  id: number;
  email: string;
  name: string;
  role: Role;
  emailEnabled: boolean;
  enabled: boolean;
}

export type GroupStatus = "NEW" | "WORKING" | "CLOSED";
export type GroupRole = "ADMIN" | "MEMBER";

export interface GroupSummary {
  id: number;
  name: string;
  ownerName: string;
  status: GroupStatus;
  myRole: GroupRole;
  hasUntaken: boolean;
  implementing: boolean;
}

export interface MemberView {
  userId: number;
  name: string;
  email: string;
  role: GroupRole;
  hasExpressions: boolean;
}

export interface GroupDetail {
  id: number;
  name: string;
  status: GroupStatus;
  myRole: GroupRole;
  members: MemberView[];
  invitations: { id: number; email: string }[];
  shareLinkEnabled: boolean;
  shareLink: string | null;
}

export type ExpressionStatus = "EXPRESSED" | "IN_PROCESS" | "PROVIDED" | "IN_POSSESSION";

export interface PersonRef {
  id: number | null;
  name: string;
}

export interface CommentView {
  id: number;
  author: PersonRef | null;
  body: string;
  systemNote: boolean;
  createdAt: string;
}

export interface ExpressionView {
  id: number;
  groupId: number;
  creator: PersonRef;
  implementer: PersonRef | null;
  status: ExpressionStatus;
  description: string;
  pictureUrl: string | null;
  pictureFromLink: boolean;
  picturePending: boolean;
  wantedBy: string | null;
  providingBy: string | null;
  incognito: boolean;
  version: number;
  canEditWish: boolean;
  canEditCare: boolean;
  canTakeCare: boolean;
  canRelease: boolean;
  canDelete: boolean;
  canMarkReceived: boolean;
  commentsOpen: boolean;
  comments: CommentView[];
}

/** A shop link as the preview card shows it. */
export interface LinkPreview {
  url: string;
  site: string;
  title: string | null;
  pictureUrl: string | null;
}

export interface ActivityView {
  groupId: number;
  groupName: string;
  myExpressions: ExpressionView[];
  myImplementations: ExpressionView[];
  notTaken: ExpressionView[];
}

export interface NotificationView {
  id: number;
  type: string;
  message: string;
  groupId: number | null;
  expressionId: number | null;
  read: boolean;
  createdAt: string;
}

export interface Inbox {
  unread: number;
  items: NotificationView[];
}
