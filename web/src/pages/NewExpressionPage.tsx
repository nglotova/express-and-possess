import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useGroup } from "../api/queries";
import type { ExpressionView } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";

export function NewExpressionPage() {
  const groupId = Number(useParams().id);
  const group = useGroup(groupId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [description, setDescription] = useState("");
  const [wantedBy, setWantedBy] = useState("");
  const create = useMutation({
    mutationFn: () =>
      api<ExpressionView>(`/api/groups/${groupId}/expressions`, "POST", {
        description,
        wantedBy: wantedBy || null,
      }),
    onSuccess: (view) => {
      queryClient.setQueryData(["expressions", view.id], view);
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      navigate(`/expressions/${view.id}`, { replace: true });
    },
  });
  function submit(e: FormEvent) {
    e.preventDefault();
    create.mutate();
  }
  return (
    <>
      <PageHeader title="New wish" parent={{ to: `/groups/${groupId}`, label: group.data?.name ?? "Group" }} />
      <form onSubmit={submit} className="stack">
        <Field label="Description" hint="Paste a link to the product if you have one; it opens in a new tab.">
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={4} required maxLength={4000} autoFocus />
        </Field>
        <Field label="By date" hint="Optional: when you would like to have it">
          <input type="date" value={wantedBy} onChange={(e) => setWantedBy(e.target.value)} />
        </Field>
        <ErrorText error={create.error} />
        <button type="submit" className="primary" disabled={create.isPending}>
          Submit
        </button>
      </form>
      <p className="muted">You can add a picture on the next screen.</p>
    </>
  );
}
