import { describe, expect, it } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ConfirmProvider, useConfirm } from "./ConfirmDialog";

function Harness({ onResult }: { onResult: (ok: boolean) => void }) {
  const ask = useConfirm();
  return (
    <button
      onClick={async () =>
        onResult(await ask({ title: "Delete this wish?", message: "Its comments go too.", confirmLabel: "Delete", danger: true }))
      }
    >
      Ask
    </button>
  );
}

describe("confirm dialog", () => {
  it("answers yes on confirm and no on Cancel or Escape, with Cancel focused for destructive actions", async () => {
    const results: boolean[] = [];
    render(
      <ConfirmProvider>
        <Harness onResult={(ok) => results.push(ok)} />
      </ConfirmProvider>,
    );

    fireEvent.click(screen.getByText("Ask"));
    expect(screen.getByRole("alertdialog", { name: "Delete this wish?" })).toHaveTextContent("Its comments go too.");
    expect(screen.getByRole("button", { name: "Cancel" })).toHaveFocus();
    fireEvent.click(screen.getByRole("button", { name: "Delete" }));
    await waitFor(() => expect(results).toEqual([true]));
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();

    fireEvent.click(screen.getByText("Ask"));
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    fireEvent.click(screen.getByText("Ask"));
    fireEvent.keyDown(document, { key: "Escape" });
    await waitFor(() => expect(results).toEqual([true, false, false]));
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });
});
