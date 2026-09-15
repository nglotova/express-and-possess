import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useGroup } from "../api/queries";
import type { ExpressionView } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";
import { LinksInput } from "../components/LinksInput";
import { prepareImage } from "../components/image";

export function NewExpressionPage() {
  const groupId = Number(useParams().id);
  const group = useGroup(groupId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [description, setDescription] = useState("");
  const [links, setLinks] = useState<string[]>([]);
  const [wantedBy, setWantedBy] = useState("");
  const [picture, setPicture] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  function choosePicture(file: File | null) {
    if (preview) URL.revokeObjectURL(preview);
    setPicture(file);
    setPreview(file ? URL.createObjectURL(file) : null);
  }
  const create = useMutation({
    mutationFn: async () => {
      const created = await api<ExpressionView>(`/api/groups/${groupId}/expressions`, "POST", {
        description,
        links: links.map((l) => l.trim()).filter((l) => l !== ""),
        wantedBy: wantedBy || null,
      });
      if (!picture) return created;
      const form = new FormData();
      form.append("file", await prepareImage(picture));
      return api<ExpressionView>(`/api/expressions/${created.id}/picture`, "POST", form);
    },
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
        <Field label="Description" hint="What it is, size, colour, anything the helper needs to know.">
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={4} required maxLength={4000} autoFocus />
        </Field>
        <LinksInput links={links} onChange={setLinks} />
        <div className="field">
          <span className="field-label">Picture</span>
          <div className="picture-pick">
            {preview ? <img src={preview} alt="" /> : <span className="muted small">none yet</span>}
            <label className="button">
              {preview ? "Choose another" : "Add or take a picture"}
              <input type="file" accept="image/*" hidden onChange={(e) => choosePicture(e.target.files?.[0] ?? null)} />
            </label>
            {preview && (
              <button type="button" className="link" onClick={() => choosePicture(null)}>
                Remove
              </button>
            )}
          </div>
          <span className="field-hint">On a phone this opens the camera or your photos. Large photos are shrunk before upload.</span>
        </div>
        <Field label="By date" hint="Optional: when you would like to have it">
          <input type="date" value={wantedBy} onChange={(e) => setWantedBy(e.target.value)} />
        </Field>
        <ErrorText error={create.error} />
        <button type="submit" className="primary" disabled={create.isPending}>
          Submit
        </button>
      </form>
    </>
  );
}
