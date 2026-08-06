import { useEffect, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import { fetchPresentationFileBlob, isPresentationPdf } from "@/lib/learning";
import { Button } from "@/components/ui/button";

interface PresentationPdfViewerProps {
  presentationId: string;
  fileId: string;
  title?: string;
  displayName?: string;
}

type ViewerState =
  | { status: "loading" }
  | { status: "ready"; url: string }
  | { status: "error" }
  | { status: "notViewable" };

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
    let objectUrl: string | null = null;

    setState({ status: "loading" });

    fetchPresentationFileBlob(presentationId, fileId)
      .then((blob) => {
        if (cancelled) return;
        // Non-empty Content-Type that is not PDF → download-only file opened via URL.
        if (blob.type && !isPresentationPdf(blob.type)) {
          setState({ status: "notViewable" });
          return;
        }
        objectUrl = URL.createObjectURL(
          blob.type ? blob : new Blob([blob], { type: "application/pdf" }),
        );
        setState({ status: "ready", url: objectUrl });
      })
      .catch(() => {
        if (!cancelled) setState({ status: "error" });
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [presentationId, fileId]);

  const goBackToLearning = () => {
    navigate({ to: "/aprendizaje" });
  };

  const heading =
    displayName || title || t("learning.presentations.viewerTitle");

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-5xl flex-col gap-4 px-4 py-6 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
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

      {state.status === "ready" && (
        <iframe
          title={heading}
          src={state.url}
          className="min-h-[65vh] w-full flex-1 rounded-md border border-border bg-background"
        />
      )}
    </div>
  );
}
