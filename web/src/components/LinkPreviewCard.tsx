import { useLinkPreview } from "../api/queries";
import { hostOf } from "./links";
import { GiftIcon } from "./GiftIcon";

/**
 * What the shop link looks like, shown while the member writes the wish: the product's
 * picture, its name, and the shop. When the description holds nothing but the link, the
 * card offers to put the product's name in front of it.
 */
export function LinkPreviewCard({ url, onUseTitle }: { url: string; onUseTitle?: (title: string) => void }) {
  const preview = useLinkPreview(url);
  const title = preview.data?.title ?? null;
  const picture = preview.data?.pictureUrl ?? null;
  const loading = preview.isPending;
  return (
    <div className="preview-card" aria-busy={loading}>
      <div className={loading ? "preview-thumb loading" : "preview-thumb"} aria-hidden="true">
        {picture ? <img src={picture} alt="" /> : !loading && <GiftIcon />}
      </div>
      <div className="preview-body">
        <span className={title ? "preview-title" : "preview-title muted"}>
          {loading ? "Looking at the link…" : (title ?? "This shop gives no preview")}
        </span>
        <a className="preview-host" href={url} target="_blank" rel="noopener noreferrer">
          ↗ {hostOf(url)}
        </a>
        {onUseTitle && title && (
          <button type="button" className="link" onClick={() => onUseTitle(title)}>
            Add this title to the description
          </button>
        )}
      </div>
    </div>
  );
}
