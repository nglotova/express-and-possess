import { useEffect, useState } from "react";
import { findLinks } from "./links";

/** The value once it has stopped changing for `ms` milliseconds. Starts with the first value. */
export function useDebounced<T>(value: T, ms: number): T {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), ms);
    return () => clearTimeout(timer);
  }, [value, ms]);
  return settled;
}

/**
 * The first link in the text once typing has paused, so a link being typed letter by
 * letter is not looked up at every keystroke. Null while it is still changing.
 */
export function useSettledFirstLink(text: string): string | null {
  const first = findLinks(text)[0] ?? null;
  const settled = useDebounced(first, 500);
  return first !== null && first === settled ? first : null;
}
