import { useEffect, useRef } from "react";
import type { PDFDocumentProxy, RenderTask } from "pdfjs-dist";
import "@/components/learning/pdf-layers.css";
import { loadPdfjs } from "@/lib/pdf";

interface Props {
  pdf: PDFDocumentProxy;
  pageNumber: number;
  /** Rendered width in CSS pixels; the height follows the page's aspect ratio. */
  width: number;
  /** Fires when the page becomes substantially visible (IntersectionObserver). */
  onPageVisible?: (page: number) => void;
  /** Selectable text + clickable links. Off for thumbnails. */
  textLayer?: boolean;
  className?: string;
}

/** One rendered PDF page (canvas, plus optional text and link layers). Browser-only. */
export function PdfSinglePage({
  pdf,
  pageNumber,
  width,
  onPageVisible,
  textLayer = true,
  className,
}: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const textLayerRef = useRef<HTMLDivElement>(null);
  const annotationLayerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!onPageVisible) return;
    const el = containerRef.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting && entry.intersectionRatio >= 0.45) {
            onPageVisible(pageNumber);
          }
        }
      },
      { threshold: [0.45, 0.6] },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [onPageVisible, pageNumber]);

  useEffect(() => {
    if (width <= 0) return;

    let cancelled = false;
    let renderTask: RenderTask | null = null;
    let cleanupLayers: (() => void) | null = null;

    (async () => {
      const pdfjs = await loadPdfjs();
      const page = await pdf.getPage(pageNumber);
      if (cancelled) return;

      const baseViewport = page.getViewport({ scale: 1 });
      const scale = width / baseViewport.width;
      const viewport = page.getViewport({ scale });

      const container = containerRef.current;
      const canvas = canvasRef.current;
      if (!container || !canvas) return;

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

      renderTask = page.render({ canvas, canvasContext: context, viewport });
      try {
        await renderTask.promise;
      } catch {
        return;
      }
      if (cancelled || !textLayer) return;

      const textLayerDiv = textLayerRef.current;
      const annotationLayerDiv = annotationLayerRef.current;
      if (!textLayerDiv || !annotationLayerDiv) return;
      const textViewport = viewport.clone({ dontFlip: true });

      textLayerDiv.replaceChildren();
      textLayerDiv.style.setProperty("--total-scale-factor", `${scale}`);
      const text = new pdfjs.TextLayer({
        textContentSource: page.streamTextContent({
          includeMarkedContent: true,
          disableNormalization: true,
        }),
        container: textLayerDiv,
        viewport: textViewport,
      });
      await text.render();
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
      cleanupLayers = () => {
        textLayerDiv.removeEventListener("mousedown", onMouseDown);
        document.removeEventListener("mouseup", onMouseUp);
        textLayerDiv.classList.remove("selecting");
        text.cancel();
      };

      const { LinkTarget, SimpleLinkService } =
        await import("pdfjs-dist/web/pdf_viewer.mjs");
      if (cancelled) return;
      const linkService = new SimpleLinkService({
        externalLinkTarget: LinkTarget.BLANK,
        externalLinkRel: "noopener noreferrer nofollow",
      });
      linkService.setDocument(pdf);

      annotationLayerDiv.replaceChildren();
      const annotations = await page.getAnnotations({ intent: "display" });
      if (cancelled) return;
      const annotationLayer = new pdfjs.AnnotationLayer({
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

      const cleanupText = cleanupLayers;
      cleanupLayers = () => {
        cleanupText();
        annotationLayer.destroy();
        linkService.setDocument(null);
      };
    })();

    return () => {
      cancelled = true;
      cleanupLayers?.();
      renderTask?.cancel();
    };
  }, [pdf, pageNumber, width, textLayer]);

  return (
    <div
      ref={containerRef}
      className={[
        "relative mx-auto max-w-full overflow-hidden border border-border bg-white shadow-sm",
        className ?? "",
      ].join(" ")}
      aria-label={`Page ${pageNumber}`}
      data-page={pageNumber}
    >
      <canvas ref={canvasRef} className="absolute inset-0 block" />
      {textLayer && (
        <>
          <div ref={textLayerRef} className="textLayer" />
          <div ref={annotationLayerRef} className="annotationLayer" />
        </>
      )}
    </div>
  );
}
