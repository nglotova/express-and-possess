import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";

export interface ConfirmOptions {
  title: string;
  message?: string;
  confirmLabel: string;
  /** A destructive action: the confirm button is red and Cancel has the focus. */
  danger?: boolean;
}

type Ask = (options: ConfirmOptions) => Promise<boolean>;

const ConfirmContext = createContext<Ask | null>(null);

/**
 * Asks the member to confirm, in the app's own dialog rather than the browser's. Resolves
 * true when they confirm, false when they cancel, press Escape, or tap outside.
 */
export function useConfirm(): Ask {
  const ask = useContext(ConfirmContext);
  if (!ask) {
    throw new Error("useConfirm needs a ConfirmProvider above it");
  }
  return ask;
}

interface Pending extends ConfirmOptions {
  resolve: (ok: boolean) => void;
}

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [pending, setPending] = useState<Pending | null>(null);
  const current = useRef<Pending | null>(null);
  current.current = pending;

  const ask = useCallback<Ask>((options) => new Promise((resolve) => setPending({ ...options, resolve })), []);
  const close = useCallback((ok: boolean) => {
    current.current?.resolve(ok);
    setPending(null);
  }, []);

  return (
    <ConfirmContext.Provider value={ask}>
      {children}
      {pending && <Dialog options={pending} onClose={close} />}
    </ConfirmContext.Provider>
  );
}

function Dialog({ options, onClose }: { options: ConfirmOptions; onClose: (ok: boolean) => void }) {
  const cancelRef = useRef<HTMLButtonElement>(null);
  const confirmRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const scroll = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    (options.danger ? cancelRef : confirmRef).current?.focus();

    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose(false);
      } else if (event.key === "Tab") {
        // Keep the focus on the two buttons while the dialog is open.
        event.preventDefault();
        const onCancel = document.activeElement === cancelRef.current;
        (onCancel ? confirmRef : cancelRef).current?.focus();
      }
    }
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = scroll;
      previous?.focus?.();
    };
  }, [options, onClose]);

  return (
    <div
      className="dialog-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose(false);
      }}
    >
      <div
        className="dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby={options.message ? "confirm-message" : undefined}
      >
        <h2 id="confirm-title">{options.title}</h2>
        {options.message && <p id="confirm-message">{options.message}</p>}
        <div className="dialog-buttons">
          <button ref={cancelRef} type="button" onClick={() => onClose(false)}>
            Cancel
          </button>
          <button
            ref={confirmRef}
            type="button"
            className={options.danger ? "danger-solid" : "primary"}
            onClick={() => onClose(true)}
          >
            {options.confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
