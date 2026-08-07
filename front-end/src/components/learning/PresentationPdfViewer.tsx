import { useEffect, useRef, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import {
  AnnotationLayer,
  getDocument,
  GlobalWorkerOptions,
  TextLayer,
  type PDFDocumentProxy,
  type RenderTask,
} from "pdfjs-dist";
import pdfWorker from "pdfjs-dist/build/pdf.worker.min.mjs?url";
import { LinkTarget, SimpleLinkService } from "pdfjs-dist/web/pdf_viewer.mjs";
import "@/components/learning/pdf-layers.css";
import { fetchPresentationFileBlob, isPresentationPdf } from "@/lib/learning";
import { Button } from "@/components/ui/button";

GlobalWorkerOptions.workerSrc = pdfWorker;

const linkService = new SimpleLinkService({
  externalLinkTarget: LinkTarget.BLANK,
  externalLinkRel: "noopener noreferrer nofollow",
});

interface PresentationPdfViewerProps {
  presentationId: string;
  fileId: string;
  title?: string;
  displayName?: string;
  /** When true, omit page chrome (back button / sticky header) for inline expand. */
  embedded?: boolean;
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
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const textLayerRef = useRef<HTMLDivElement>(null);
  const annotationLayerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (width <= 0) return;

    let cancelled = false;
    let renderTask: RenderTask | null = null;
    let textLayer: TextLayer | null = null;
    let annotationLayer: AnnotationLayer | null = null;
    let cleanupSelection: (() => void) | null = null;

    (async () => {
      const page = await pdf.getPage(pageNumber);
      if (cancelled) return;

      const baseViewport = page.getViewport({ scale: 1 });
      const scale = width / baseViewport.width;
      const viewport = page.getViewport({ scale });

      const container = containerRef.current;
      const canvas = canvasRef.current;
      const textLayerDiv = textLayerRef.current;
      const annotationLayerDiv = annotationLayerRef.current;
      if (!container || !canvas || !textLayerDiv || !annotationLayerDiv) return;

      container.style.width = `${Math.floor(viewport.width)}px`;
      container.style.height = `${Math.floor(viewport.height)}px`;

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
        return;
      }
      if (cancelled) return;

      const textViewport = viewport.clone({ dontFlip: true });

      textLayerDiv.replaceChildren();
      textLayerDiv.style.setProperty("--total-scale-factor", `${scale}`);
      textLayer = new TextLayer({
        textContentSource: page.streamTextContent({
          includeMarkedContent: true,
          disableNormalization: true,
        }),
        container: textLayerDiv,
        viewport: textViewport,
      });
      await textLayer.render();
      if (cancelled) return;

      // Same guard PDF.js's TextLayerBuilder uses so selection can't spill
      // past the last line into text below (or the next page).
      const endOfContent = document.createElement("div");
      endOfContent.className = "endOfContent";
      textLayerDiv.append(endOfContent);
      const onMouseDown = () => textLayerDiv.classList.add("selecting");
      const onMouseUp = () => textLayerDiv.classList.remove("selecting");
      textLayerDiv.addEventListener("mousedown", onMouseDown);
      document.addEventListener("mouseup", onMouseUp);
      cleanupSelection = () => {
        textLayerDiv.removeEventListener("mousedown", onMouseDown);
        document.removeEventListener("mouseup", onMouseUp);
        textLayerDiv.classList.remove("selecting");
      };

      annotationLayerDiv.replaceChildren();
      const annotations = await page.getAnnotations({ intent: "display" });
      if (cancelled) return;

      annotationLayer = new AnnotationLayer({
        div: annotationLayerDiv,
        page,
        viewport: textViewport,
        linkService,
      });
      await annotationLayer.render({
        annotations,
        viewport: textViewport,
        linkService,
        renderForms: false,
      });
    })();

    return () => {
      cancelled = true;
      cleanupSelection?.();
      renderTask?.cancel();
      textLayer?.cancel();
      annotationLayer?.destroy();
    };
  }, [pdf, pageNumber, width]);

  return (
    <div
      ref={containerRef}
      className="relative mx-auto max-w-full overflow-hidden border border-border bg-white shadow-sm"
      aria-label={`Page ${pageNumber}`}
    >
      <canvas ref={canvasRef} className="absolute inset-0 block" />
      <div ref={textLayerRef} className="textLayer" />
      <div ref={annotationLayerRef} className="annotationLayer" />
    </div>
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

  useEffect(() => {
    linkService.setDocument(pdf);
    return () => {
      linkService.setDocument(null);
    };
  }, [pdf]);

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
  embedded = false,
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

  const body = (
    <>
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
    </>
  );

  if (embedded) {
    return <div className="w-full">{body}</div>;
  }

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
      {body}
    </div>
  );
}
