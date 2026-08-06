import { useEffect, useRef, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import {
  getDocument,
  GlobalWorkerOptions,
  type PDFDocumentProxy,
  type RenderTask,
} from "pdfjs-dist";
import pdfWorker from "pdfjs-dist/build/pdf.worker.min.mjs?url";
import { fetchPresentationFileBlob, isPresentationPdf } from "@/lib/learning";
import { Button } from "@/components/ui/button";

GlobalWorkerOptions.workerSrc = pdfWorker;

interface PresentationPdfViewerProps {
  presentationId: string;
  fileId: string;
  title?: string;
  displayName?: string;
}

type ViewerState =
  | { status: "loading" }
  | { status: "ready"; pdf: PDFDocumentProxy }
  | { status: "error" }
  | { status: "notViewable" };

function PdfPage({
  pdf,
  pageNumber,
  width,
}: {
  pdf: PDFDocumentProxy;
  pageNumber: number;
  width: number;
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (width <= 0) return;

    let cancelled = false;
    let renderTask: RenderTask | null = null;

    (async () => {
      const page = await pdf.getPage(pageNumber);
      if (cancelled) return;

      const baseViewport = page.getViewport({ scale: 1 });
      const scale = width / baseViewport.width;
      const viewport = page.getViewport({ scale });
      const canvas = canvasRef.current;
      if (!canvas) return;

      const context = canvas.getContext("2d");
      if (!context) return;

      const outputScale = window.devicePixelRatio || 1;
      canvas.width = Math.floor(viewport.width * outputScale);
      canvas.height = Math.floor(viewport.height * outputScale);
      canvas.style.width = `${viewport.width}px`;
      canvas.style.height = `${viewport.height}px`;
      context.setTransform(outputScale, 0, 0, outputScale, 0, 0);

      renderTask = page.render({
        canvas,
        canvasContext: context,
        viewport,
      });
      try {
        await renderTask.promise;
      } catch {
        // Cancelled renders throw; ignore when unmounting / resizing.
      }
    })();

    return () => {
      cancelled = true;
      renderTask?.cancel();
    };
  }, [pdf, pageNumber, width]);

  return (
    <canvas
      ref={canvasRef}
      className="mx-auto max-w-full border border-border bg-white shadow-sm"
      aria-label={`Page ${pageNumber}`}
    />
  );
}

function PdfPageStack({ pdf }: { pdf: PDFDocumentProxy }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const update = () => setWidth(el.clientWidth);
    update();

    const observer = new ResizeObserver(update);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <div ref={containerRef} className="flex flex-col gap-6 pb-10">
      {width > 0 &&
        Array.from({ length: pdf.numPages }, (_, i) => (
          <PdfPage key={i + 1} pdf={pdf} pageNumber={i + 1} width={width} />
        ))}
    </div>
  );
}

export function PresentationPdfViewer({
  presentationId,
  fileId,
  title,
  displayName,
}: PresentationPdfViewerProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [state, setState] = useState<ViewerState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;
    let loadingTask: ReturnType<typeof getDocument> | null = null;

    setState({ status: "loading" });

    (async () => {
      try {
        const blob = await fetchPresentationFileBlob(presentationId, fileId);
        if (cancelled) return;

        if (blob.type && !isPresentationPdf(blob.type)) {
          setState({ status: "notViewable" });
          return;
        }

        const data = await blob.arrayBuffer();
        if (cancelled) return;

        loadingTask = getDocument({ data });
        const pdf = await loadingTask.promise;
        if (cancelled) {
          void loadingTask.destroy();
          return;
        }
        setState({ status: "ready", pdf });
      } catch {
        if (!cancelled) setState({ status: "error" });
      }
    })();

    return () => {
      cancelled = true;
      void loadingTask?.destroy();
    };
  }, [presentationId, fileId]);

  const goBackToLearning = () => {
    navigate({ to: "/aprendizaje" });
  };

  const heading =
    displayName || title || t("learning.presentations.viewerTitle");

  return (
    <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6">
      <div className="sticky top-0 z-10 -mx-4 mb-6 flex flex-wrap items-center justify-between gap-3 border-b border-border/60 bg-background/95 px-4 py-3 backdrop-blur sm:-mx-6 sm:px-6">
        <h1 className="font-display truncate text-lg font-semibold text-foreground">
          {heading}
        </h1>
        <Button variant="outline" size="sm" onClick={goBackToLearning}>
          {t("learning.presentations.backToLearning")}
        </Button>
      </div>

      {state.status === "loading" && (
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("learning.presentations.viewing")}
        </p>
      )}

      {state.status === "error" && (
        <p className="text-sm text-destructive">
          {t("learning.presentations.loadError")}
        </p>
      )}

      {state.status === "notViewable" && (
        <p className="text-sm text-muted-foreground">
          {t("learning.presentations.notViewable")}
        </p>
      )}

      {state.status === "ready" && <PdfPageStack pdf={state.pdf} />}
    </div>
  );
}
