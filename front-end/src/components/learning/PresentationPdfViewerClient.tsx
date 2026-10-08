import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import type { PDFDocumentProxy } from "pdfjs-dist";
import {
  fetchPresentationFileBlob,
  isPresentationPdf,
  type ActivitySummary,
} from "@/lib/learning";
import { usePdfDocument } from "@/lib/pdf";
import { Button } from "@/components/ui/button";
import { ActivityPageSlot } from "./ActivityPageSlot";
import { PdfSinglePage } from "./PdfSinglePage";
import type { PresentationPdfViewerProps } from "./PresentationPdfViewer";

function PdfPageStack({
  pdf,
  onPageVisible,
  activitiesByPage,
  onActivityChanged,
}: {
  pdf: PDFDocumentProxy;
  onPageVisible?: (page: number) => void;
  activitiesByPage: Map<number, ActivitySummary>;
  onActivityChanged?: (activityId: string) => void;
}) {
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
        Array.from({ length: pdf.numPages }, (_, i) => {
          const pageNumber = i + 1;
          const activity = activitiesByPage.get(pageNumber);
          return (
            <div key={pageNumber} className="flex flex-col gap-3">
              <PdfSinglePage
                pdf={pdf}
                pageNumber={pageNumber}
                width={width}
                onPageVisible={onPageVisible}
              />
              {activity && (
                <ActivityPageSlot
                  activity={activity}
                  onChanged={() => onActivityChanged?.(activity.id)}
                />
              )}
            </div>
          );
        })}
    </div>
  );
}

/** Browser-only PDF.js viewer — do not import from SSR modules. */
export function PresentationPdfViewerClient({
  presentationId,
  fileId,
  title,
  displayName,
  embedded = false,
  onPageVisible,
  activities = [],
  onActivityChanged,
}: PresentationPdfViewerProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [notViewable, setNotViewable] = useState(false);

  const state = usePdfDocument(async () => {
    const blob = await fetchPresentationFileBlob(presentationId, fileId);
    if (blob.type && !isPresentationPdf(blob.type)) {
      setNotViewable(true);
      throw new Error("not a pdf");
    }
    return blob;
  }, `${presentationId}:${fileId}`);

  // A page activity sits right after its page (the page above is its instructions).
  const activitiesByPage = useMemo(
    () => new Map(activities.map((a) => [a.page, a])),
    [activities],
  );

  const heading =
    displayName || title || t("learning.presentations.viewerTitle");

  const body = (
    <>
      {notViewable ? (
        <p className="text-sm text-muted-foreground">
          {t("learning.presentations.notViewable")}
        </p>
      ) : state.status === "loading" ? (
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("learning.presentations.viewing")}
        </p>
      ) : state.status === "error" ? (
        <p className="text-sm text-destructive">
          {t("learning.presentations.loadError")}
        </p>
      ) : (
        <PdfPageStack
          pdf={state.pdf}
          onPageVisible={onPageVisible}
          activitiesByPage={activitiesByPage}
          onActivityChanged={onActivityChanged}
        />
      )}
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
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate({ to: "/aprendizaje" })}
        >
          {t("learning.presentations.backToLearning")}
        </Button>
      </div>
      {body}
    </div>
  );
}
