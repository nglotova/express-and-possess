import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "./client";
import type { ActivityView, ExpressionView, GroupDetail, GroupSummary, Inbox, LinkPreview, User } from "./types";

/** While the server fetches a picture from a link, pages showing that wish ask again this often. */
const PICTURE_POLL_MS = 3000;

// ---- the logged-in member ----------------------------------------------------------

export function useMe() {
  return useQuery({
    queryKey: ["me"],
    queryFn: async () => {
      try {
        return await api<User>("/api/me");
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          return null;
        }
        throw e;
      }
    },
    staleTime: 60_000,
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api<void>("/api/auth/logout", "POST"),
    onSuccess: () => queryClient.clear(),
  });
}

// ---- groups -------------------------------------------------------------------------

export function useGroups() {
  return useQuery({ queryKey: ["groups"], queryFn: () => api<GroupSummary[]>("/api/groups") });
}

export function useGroup(id: number) {
  return useQuery({ queryKey: ["groups", id], queryFn: () => api<GroupDetail>(`/api/groups/${id}`) });
}

export function useActivity(groupId: number) {
  return useQuery({
    queryKey: ["groups", groupId, "activity"],
    queryFn: () => api<ActivityView>(`/api/groups/${groupId}/activity`),
    refetchInterval: (query) => {
      const a = query.state.data;
      const pending = a && [...a.myExpressions, ...a.myImplementations, ...a.notTaken].some((e) => e.picturePending);
      return pending ? PICTURE_POLL_MS : false;
    },
  });
}

export function useMemberExpressions(groupId: number, userId: number) {
  return useQuery({
    queryKey: ["groups", groupId, "members", userId, "expressions"],
    queryFn: () => api<ExpressionView[]>(`/api/groups/${groupId}/members/${userId}/expressions`),
    refetchInterval: (query) => (query.state.data?.some((e) => e.picturePending) ? PICTURE_POLL_MS : false),
  });
}

/** Any change to a group refreshes its pages and the My Groups list. */
export function useInvalidateGroups() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["groups"] });
}

// ---- expressions --------------------------------------------------------------------

export function useExpression(id: number) {
  return useQuery({
    queryKey: ["expressions", id],
    queryFn: () => api<ExpressionView>(`/api/expressions/${id}`),
    refetchInterval: (query) => (query.state.data?.picturePending ? PICTURE_POLL_MS : false),
  });
}

/** Every expression action returns the fresh view; store it and refresh the lists. */
export function useExpressionAction<TVars>(
  id: number,
  action: (vars: TVars) => Promise<ExpressionView>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: action,
    onSuccess: (view) => {
      queryClient.setQueryData(["expressions", id], view);
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

/** The preview of a shop link; the server keeps each for an hour, so the page does too. */
export function useLinkPreview(url: string | null) {
  return useQuery({
    queryKey: ["link-preview", url],
    queryFn: () => api<LinkPreview>(`/api/link-preview?url=${encodeURIComponent(url ?? "")}`),
    enabled: url !== null,
    staleTime: 60 * 60 * 1000,
    retry: false,
  });
}

// ---- notifications -----------------------------------------------------------------

export function useUnreadCount() {
  return useQuery({
    queryKey: ["notifications", "unread"],
    queryFn: () => api<{ unread: number }>("/api/notifications/unread-count"),
    refetchInterval: 30_000,
  });
}

export function useInbox() {
  return useQuery({ queryKey: ["notifications", "inbox"], queryFn: () => api<Inbox>("/api/notifications") });
}
