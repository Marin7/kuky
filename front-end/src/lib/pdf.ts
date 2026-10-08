import { useEffect, useState } from "react";
import type { PDFDocumentProxy } from "pdfjs-dist";

/**
 * PDF.js helpers. pdfjs-dist uses DOMMatrix and must never be evaluated during SSR,
 * so it is only ever loaded through a dynamic import from browser-side code.
 */

type PdfJs = typeof import("pdfjs-dist");

let pdfjsPromise: Promise<PdfJs> | null = null;

/** Loads pdfjs-dist once (browser only) with its worker configured. */
export function loadPdfjs(): Promise<PdfJs> {
  if (!pdfjsPromise) {
    pdfjsPromise = Promise.all([
      import("pdfjs-dist"),
      import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
    ]).then(([pdfjs, worker]) => {
      pdfjs.GlobalWorkerOptions.workerSrc = worker.default;
      return pdfjs;
    });
  }
  return pdfjsPromise;
}

export function isPdfFile(file: File): boolean {
  return (
    file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf")
  );
}

/** Page count of a local PDF file (read before upload — the server stores it). */
export async function countPdfPages(file: File): Promise<number> {
  const pdfjs = await loadPdfjs();
  const task = pdfjs.getDocument({ data: await file.arrayBuffer() });
  try {
    const pdf = await task.promise;
    return pdf.numPages;
  } finally {
    void task.destroy();
  }
}

export type PdfDocumentState =
  | { status: "loading" }
  | { status: "ready"; pdf: PDFDocumentProxy }
  | { status: "error" };

/**
 * Loads a PDF document from `load()` (a Blob or bytes) and destroys it on change/unmount.
 * Pass `null` as the loader to stay idle. `key` reloads when it changes.
 */
export function usePdfDocument(
  load: (() => Promise<Blob | ArrayBuffer>) | null,
  key: string | null,
): PdfDocumentState {
  const [state, setState] = useState<PdfDocumentState>({ status: "loading" });

  useEffect(() => {
    if (!load || key == null) return;
    let cancelled = false;
    let task: { destroy: () => Promise<void> } | null = null;
    setState({ status: "loading" });

    (async () => {
      try {
        const [pdfjs, source] = await Promise.all([loadPdfjs(), load()]);
        const data =
          source instanceof Blob ? await source.arrayBuffer() : source;
        if (cancelled) return;
        const loadingTask = pdfjs.getDocument({ data });
        task = loadingTask;
        const pdf = await loadingTask.promise;
        if (!cancelled) setState({ status: "ready", pdf });
      } catch {
        if (!cancelled) setState({ status: "error" });
      }
    })();

    return () => {
      cancelled = true;
      void task?.destroy();
    };
    // `load` is recreated every render by callers; `key` identifies the document.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return state;
}
