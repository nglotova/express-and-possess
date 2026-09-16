import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useGroup, useLinkPreview } from "../api/queries";
import type { ExpressionView } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";
import { LinkPreviewCard } from "../components/LinkPreviewCard";
import { LinkList, findLinks, withoutLinks } from "../components/links";
import { prepareImage } from "../components/image";
import { useSettledFirstLink } from "../components/useDebounced";

export function NewExpressionPage() {
  const groupId = Number(useParams().id);
  const group = useGroup(groupId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [description, setDescription] = useState("");
  const [wantedBy, setWantedBy] = useState("");
  const [ownPicture, setOwnPicture] = useState<File | null>(null);
  const [ownPreview, setOwnPreview] = useState<string | null>(null);
  const links = findLinks(description);
  const firstLink = useSettledFirstLink(description);
  const linkPicture = useLinkPreview(firstLink).data?.pictureUrl ?? null;

  function choosePicture(file: File | null) {
    if (ownPreview) URL.revokeObjectURL(ownPreview);
    setOwnPicture(file);
    setOwnPreview(file ? URL.createObjectURL(file) : null);
  }
  const create = useMutation({
    mutationFn: async () => {
      const created = await api<ExpressionView>(`/api/groups/${groupId}/expressions`, "POST", {
        description,
        wantedBy: wantedBy || null,
      });
      if (!ownPicture) return created;
      const form = new FormData();
      form.append("file", await prepareImage(ownPicture));
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

  const onlyLinks = withoutLinks(description) === "";
  let pictureHint = "On a phone this opens the camera or your photos.";
  if (!ownPreview && linkPicture) pictureHint = "The picture from the link is used unless you add your own.";
  else if (!ownPreview && links.length > 0) pictureHint = "Leave it empty and the picture is taken from the link, if the shop allows it.";

  return (
    <>
      <PageHeader title="New wish" parent={{ to: `/groups/${groupId}`, label: group.data?.name ?? "Group" }} />
      <form onSubmit={submit} className="stack">
        <Field label="Description" hint="What it is, size, colour. Paste the shop's link right into the text.">
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={4} required maxLength={4000} autoFocus />
        </Field>
        {firstLink && (
          <LinkPreviewCard
            url={firstLink}
            onUseTitle={onlyLinks ? (title) => setDescription((d) => `${title}\n${d.trim()}`) : undefined}
          />
        )}
        <LinkList links={firstLink ? links.filter((l) => l !== firstLink) : links} />
        <div className="field">
          <span className="field-label">Picture</span>
          <div className="picture-pick">
            {ownPreview ? (
              <img src={ownPreview} alt="" />
            ) : linkPicture ? (
              <img src={linkPicture} alt="" />
            ) : (
              <span className="muted small">none yet</span>
            )}
            <label className="button">
              {ownPreview ? "Choose another" : linkPicture ? "Use my own picture" : "Add or take a picture"}
              <input type="file" accept="image/*" hidden onChange={(e) => choosePicture(e.target.files?.[0] ?? null)} />
            </label>
            {ownPreview && (
              <button type="button" className="link" onClick={() => choosePicture(null)}>
                Remove
              </button>
            )}
          </div>
          <span className="field-hint">{pictureHint}</span>
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
