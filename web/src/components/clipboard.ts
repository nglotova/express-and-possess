import { useEffect, useRef } from "react";

/** What the page says when the clipboard holds no picture. */
export const NO_PICTURE_ON_CLIPBOARD =
  "There is no picture on the clipboard. Copy one first: on a phone, press and hold the picture and choose Copy.";

function asFile(blob: Blob, type: string): File {
  return new File([blob], `pasted.${type.split("/")[1] ?? "png"}`, { type });
}

/**
 * The picture on the clipboard, or null when it holds none. Must run from a click: the
 * browser may ask the member first (Safari shows a Paste button, Chrome asks once).
 */
export async function pictureFromClipboard(): Promise<File | null> {
  if (!navigator.clipboard?.read) {
    return null;
  }
  for (const item of await navigator.clipboard.read()) {
    const type = item.types.find((t) => t.startsWith("image/"));
    if (type) {
      return asFile(await item.getType(type), type);
    }
  }
  return null;
}

/** The first picture in a paste, or null when the paste is text. */
export function pictureInPaste(data: DataTransfer | null): File | null {
  if (!data) {
    return null;
  }
  for (const file of Array.from(data.files ?? [])) {
    if (file.type.startsWith("image/")) {
      return file;
    }
  }
  return null;
}

/**
 * ⌘V or Ctrl+V anywhere on the page with a picture on the clipboard hands the picture over.
 * A paste of text is left alone, so the description can still be pasted into.
 */
export function usePastedPicture(onPicture: (file: File) => void, enabled = true) {
  const handler = useRef(onPicture);
  handler.current = onPicture;
  useEffect(() => {
    if (!enabled) {
      return;
    }
    function onPaste(event: ClipboardEvent) {
      const file = pictureInPaste(event.clipboardData);
      if (file) {
        event.preventDefault();
        handler.current(file);
      }
    }
    document.addEventListener("paste", onPaste);
    return () => document.removeEventListener("paste", onPaste);
  }, [enabled]);
}
