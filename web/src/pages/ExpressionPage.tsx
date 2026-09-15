import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useExpression, useExpressionAction, useGroup } from "../api/queries";
import type { ExpressionView } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";
import { StatusChip } from "../components/StatusChip";
import { formatDate, formatTime } from "../components/format";
import { linkify } from "../components/links";
import { LinkList, LinksInput } from "../components/LinksInput";
import { prepareImage } from "../components/image";

/**
 * The expression page: the wish (creator edits), taking care (implementer edits), and the
 * comments. Which controls appear comes from the view's can* flags; the server checks the
 * same rules again.
 */
export function ExpressionPage() {
  const id = Number(useParams().id);
  const expression = useExpression(id);
  if (expression.isPending) return <p className="muted">Loading…</p>;
  if (expression.isError || !expression.data) return <p className="error">This wish is not available.</p>;
  const e = expression.data;
  return (
    <>
      <Header expression={e} />
      <WishSection expression={e} />
      <CareSection key={`${e.status}:${e.implementer?.id ?? ""}`} expression={e} />
      <Comments expression={e} />
    </>
  );
}

function Header({ expression: e }: { expression: ExpressionView }) {
  const group = useGroup(e.groupId);
  return (
    <PageHeader
      title={firstLine(e.description)}
      parent={{ to: `/groups/${e.groupId}`, label: group.data?.name ?? "Group" }}
      action={<StatusChip status={e.status} />}
    />
  );
}

function WishSection({ expression: e }: { expression: ExpressionView }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [description, setDescription] = useState(e.description);
  const [links, setLinks] = useState<string[]>(e.links);
  const [wantedBy, setWantedBy] = useState(e.wantedBy ?? "");
  const [gotIt, setGotIt] = useState(false);
  useEffect(() => {
    setDescription(e.description);
    setLinks(e.links);
    setWantedBy(e.wantedBy ?? "");
  }, [e.description, e.links, e.wantedBy]);
  const cleanLinks = links.map((l) => l.trim()).filter((l) => l !== "");

  const save = useExpressionAction(e.id, () =>
    api<ExpressionView>(`/api/expressions/${e.id}/wish`, "PUT", {
      description,
      links: cleanLinks,
      wantedBy: wantedBy || null,
      version: e.version,
    }),
  );
  const received = useExpressionAction(e.id, () => api<ExpressionView>(`/api/expressions/${e.id}/received`, "POST"));
  const picture = useExpressionAction(e.id, async (file: File) => {
    const form = new FormData();
    form.append("file", await prepareImage(file));
    return api<ExpressionView>(`/api/expressions/${e.id}/picture`, "POST", form);
  });
  const remove = useMutation({
    mutationFn: () => api<void>(`/api/expressions/${e.id}`, "DELETE"),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      navigate(`/groups/${e.groupId}`, { replace: true });
    },
  });

  function submit(ev: FormEvent) {
    ev.preventDefault();
    if (gotIt) received.mutate(undefined);
    else save.mutate(undefined);
  }

  const descriptionLocked = e.status !== "EXPRESSED";
  const changed =
    description !== e.description ||
    cleanLinks.join("\n") !== e.links.join("\n") ||
    (wantedBy || null) !== e.wantedBy ||
    gotIt;
  return (
    <section className="section">
      <div className="section-head">
        <h2>Wish</h2>
        <span className="muted">by {e.creator.name}</span>
      </div>
      <div className="wish-body">
        <div className="picture">
          {e.pictureUrl ? <img src={e.pictureUrl} alt="" /> : <span className="muted">no picture</span>}
          {e.canEditWish && (
            <label className="link file">
              {picture.isPending ? "Uploading…" : e.pictureUrl ? "Replace picture" : "Add or take a picture"}
              <input
                type="file"
                accept="image/*"
                hidden
                onChange={(ev) => {
                  const file = ev.target.files?.[0];
                  if (file) picture.mutate(file);
                }}
              />
            </label>
          )}
        </div>
        {e.canEditWish ? (
          <form onSubmit={submit} className="stack grow">
            <Field label="Description" hint={descriptionLocked ? "Locked while someone takes care of it" : undefined}>
              <textarea
                value={description}
                onChange={(ev) => setDescription(ev.target.value)}
                rows={4}
                required
                maxLength={4000}
                disabled={descriptionLocked}
              />
            </Field>
            {descriptionLocked ? <LinkList links={e.links} /> : <LinksInput links={links} onChange={setLinks} />}
            <Field label="By date">
              <input type="date" value={wantedBy} onChange={(ev) => setWantedBy(ev.target.value)} />
            </Field>
            {e.canMarkReceived && (
              <label className="check">
                <input type="checkbox" checked={gotIt} onChange={(ev) => setGotIt(ev.target.checked)} />
                Got it
              </label>
            )}
            <ErrorText error={save.error ?? received.error ?? picture.error ?? remove.error} />
            <div className="button-row">
              <button type="submit" className="primary" disabled={!changed || save.isPending || received.isPending}>
                {gotIt ? "Confirm: got it" : "Save"}
              </button>
              {save.isSuccess && !changed && <span className="muted">Saved.</span>}
              {e.canDelete && (
                <button
                  type="button"
                  className="link danger"
                  onClick={() => {
                    if (confirm("Delete this wish and its comments?")) remove.mutate();
                  }}
                >
                  Delete
                </button>
              )}
            </div>
          </form>
        ) : (
          <div className="grow">
            <p className="prose">{linkify(e.description)}</p>
            <LinkList links={e.links} />
            {e.wantedBy && <p className="muted">Wanted by {formatDate(e.wantedBy)}</p>}
          </div>
        )}
      </div>
    </section>
  );
}

function CareSection({ expression: e }: { expression: ExpressionView }) {
  const [incognitoAtClaim, setIncognitoAtClaim] = useState(false);
  const [incognito, setIncognito] = useState(e.incognito);
  const [providingBy, setProvidingBy] = useState(e.providingBy ?? "");
  const [provided, setProvided] = useState(false);
  useEffect(() => {
    setIncognito(e.incognito);
    setProvidingBy(e.providingBy ?? "");
  }, [e.incognito, e.providingBy]);

  const takeCare = useExpressionAction(e.id, () =>
    api<ExpressionView>(`/api/expressions/${e.id}/take-care`, "POST", { incognito: incognitoAtClaim }),
  );
  const save = useExpressionAction(e.id, () =>
    api<ExpressionView>(`/api/expressions/${e.id}/care`, "PUT", {
      incognito,
      providingBy: providingBy || null,
      provided,
      version: e.version,
    }),
  );
  const release = useExpressionAction(e.id, () => api<ExpressionView>(`/api/expressions/${e.id}/release`, "POST"));
  const changed = incognito !== e.incognito || (providingBy || null) !== e.providingBy || provided;

  if (e.canTakeCare) {
    return (
      <section className="section">
        <h2>Taking care</h2>
        <p className="muted">Nobody has taken care of this wish yet.</p>
        <label className="check">
          <input type="checkbox" checked={incognitoAtClaim} onChange={(ev) => setIncognitoAtClaim(ev.target.checked)} />
          Incognito: hide my name from the others
        </label>
        <ErrorText error={takeCare.error} />
        <button className="primary" onClick={() => takeCare.mutate(undefined)} disabled={takeCare.isPending}>
          Take care
        </button>
      </section>
    );
  }
  if (!e.implementer) {
    return null;
  }
  return (
    <section className="section">
      <div className="section-head">
        <h2>Taken care of by</h2>
        <span>{e.implementer.name}</span>
      </div>
      {e.canEditCare ? (
        <form
          onSubmit={(ev) => {
            ev.preventDefault();
            save.mutate(undefined);
          }}
          className="stack"
        >
          <label className="check">
            <input type="checkbox" checked={incognito} onChange={(ev) => setIncognito(ev.target.checked)} />
            Incognito
          </label>
          <Field label="Providing by date" hint="Optional: when you expect to have it">
            <input type="date" value={providingBy} onChange={(ev) => setProvidingBy(ev.target.value)} />
          </Field>
          <label className="check">
            <input type="checkbox" checked={provided} onChange={(ev) => setProvided(ev.target.checked)} />
            Provided: it is bought or made
          </label>
          <ErrorText error={save.error ?? release.error} />
          <div className="button-row">
            <button type="submit" className="primary" disabled={!changed || save.isPending}>
              {provided ? "Confirm: provided" : "Save"}
            </button>
            {save.isSuccess && !changed && <span className="muted">Saved.</span>}
            {e.canRelease && (
              <button
                type="button"
                className="link danger"
                onClick={() => {
                  if (confirm("Stop taking care of this wish? It goes back to the group.")) release.mutate(undefined);
                }}
              >
                Release
              </button>
            )}
          </div>
        </form>
      ) : (
        <p className="muted">{e.providingBy ? `Providing by ${formatDate(e.providingBy)}` : "No date set"}</p>
      )}
    </section>
  );
}

function Comments({ expression: e }: { expression: ExpressionView }) {
  const [body, setBody] = useState("");
  const send = useExpressionAction(e.id, () => api<ExpressionView>(`/api/expressions/${e.id}/comments`, "POST", { body }));
  return (
    <section className="section">
      <h2>Comments</h2>
      {e.comments.length === 0 && <p className="muted">No comments yet.</p>}
      <ul className="comments">
        {e.comments.map((c) => (
          <li key={c.id} className={c.systemNote ? "comment system" : "comment"}>
            <div className="comment-meta">
              {c.systemNote ? "System" : c.author?.name} · {formatTime(c.createdAt)}
            </div>
            <div>{c.body}</div>
          </li>
        ))}
      </ul>
      {e.commentsOpen ? (
        <form
          onSubmit={(ev) => {
            ev.preventDefault();
            send.mutate(undefined, { onSuccess: () => setBody("") });
          }}
          className="inline-form"
        >
          <textarea value={body} onChange={(ev) => setBody(ev.target.value)} rows={2} required maxLength={2000} placeholder="Write a comment" />
          <button type="submit" disabled={send.isPending}>
            Send
          </button>
          <ErrorText error={send.error} />
        </form>
      ) : (
        <p className="muted">Comments are closed.</p>
      )}
    </section>
  );
}

function firstLine(text: string) {
  const line = text.trim().split("\n")[0];
  return line.length > 60 ? line.slice(0, 57) + "…" : line;
}
