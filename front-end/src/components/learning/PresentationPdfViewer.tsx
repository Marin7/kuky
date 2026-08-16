import { useEffect, useState, type ComponentType } from "react";
import { useTranslation } from "react-i18next";
import type { ActivitySummary } from "@/lib/learning";

export interface PresentationPdfViewerProps {
  presentationId: string;
  fileId: string;
  title?: string;
  displayName?: string;
  embedded?: boolean;
  onPageVisible?: (page: number) => void;
  activities?: ActivitySummary[];
  onActivityChanged?: (activityId: string) => void;
}

/**
 * Loads PDF.js only in the browser. The modern pdfjs-dist build uses DOMMatrix
 * and must not be evaluated during Vite/TanStack Start SSR (or HMR of the
 * route graph, which re-runs SSR modules).
 */
export function PresentationPdfViewer(props: PresentationPdfViewerProps) {
  const { t } = useTranslation();
  const [Client, setClient] =
    useState<ComponentType<PresentationPdfViewerProps> | null>(null);

  useEffect(() => {
    let cancelled = false;
    void import("./PresentationPdfViewerClient").then((mod) => {
      if (!cancelled) setClient(() => mod.PresentationPdfViewerClient);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!Client) {
    const loading = (
      <p className="animate-pulse text-sm text-muted-foreground">
        {t("learning.presentations.viewing")}
      </p>
    );
    if (props.embedded) return <div className="w-full">{loading}</div>;
    return <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6">{loading}</div>;
  }

  return <Client {...props} />;
}
