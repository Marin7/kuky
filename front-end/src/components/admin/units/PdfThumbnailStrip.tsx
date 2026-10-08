import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import type { PDFDocumentProxy } from "pdfjs-dist";
import { PdfSinglePage } from "@/components/learning/PdfSinglePage";
import { NotificationDot } from "@/components/NotificationDot";

const THUMB_WIDTH = 96;

interface Props {
  pdf: PDFDocumentProxy;
  selectedPage: number;
  onSelect: (page: number) => void;
  /** Pages marked as activities (badge). */
  activityPages: Set<number>;
  /** Activity pages with student work the teacher has not opened yet. */
  unseenPages: Set<number>;
}

/** Horizontal strip of page thumbnails; each renders only once scrolled into view. */
export function PdfThumbnailStrip({
  pdf,
  selectedPage,
  onSelect,
  activityPages,
  unseenPages,
}: Props) {
  const stripRef = useRef<HTMLDivElement>(null);

  // Keep the selected page in view when it changes from outside (e.g. activity list).
  useEffect(() => {
    stripRef.current
      ?.querySelector<HTMLElement>(`[data-thumb="${selectedPage}"]`)
      ?.scrollIntoView({ block: "nearest", inline: "nearest" });
  }, [selectedPage]);

  return (
    <div
      ref={stripRef}
      className="flex gap-2 overflow-x-auto rounded-md border bg-muted/30 p-2"
      role="listbox"
    >
      {Array.from({ length: pdf.numPages }, (_, i) => i + 1).map((page) => (
        <Thumbnail
          key={page}
          pdf={pdf}
          page={page}
          selected={page === selectedPage}
          isActivity={activityPages.has(page)}
          unseen={unseenPages.has(page)}
          onSelect={() => onSelect(page)}
        />
      ))}
    </div>
  );
}

function Thumbnail({
  pdf,
  page,
  selected,
  isActivity,
  unseen,
  onSelect,
}: {
  pdf: PDFDocumentProxy;
  page: number;
  selected: boolean;
  isActivity: boolean;
  unseen: boolean;
  onSelect: () => void;
}) {
  const { t } = useTranslation();
  const ref = useRef<HTMLButtonElement>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el || visible) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) setVisible(true);
      },
      { rootMargin: "200px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [visible]);

  return (
    <button
      ref={ref}
      type="button"
      role="option"
      aria-selected={selected}
      data-thumb={page}
      onClick={onSelect}
      className={[
        "relative flex shrink-0 flex-col items-center gap-1 rounded-md p-1 transition-colors",
        selected ? "bg-primary/15 ring-2 ring-primary" : "hover:bg-muted",
      ].join(" ")}
      style={{ width: THUMB_WIDTH + 8 }}
    >
      <div
        className="flex items-center justify-center overflow-hidden"
        style={{ width: THUMB_WIDTH, minHeight: Math.round(THUMB_WIDTH * 0.6) }}
      >
        {visible && (
          <PdfSinglePage
            pdf={pdf}
            pageNumber={page}
            width={THUMB_WIDTH}
            textLayer={false}
            className="pointer-events-none"
          />
        )}
      </div>
      <span className="flex items-center gap-1 text-[11px] text-muted-foreground">
        {page}
        {isActivity && (
          <span className="rounded bg-primary px-1 text-[10px] font-semibold text-primary-foreground">
            {t("admin.units.editor.activityBadge")}
          </span>
        )}
      </span>
      {unseen && (
        <span className="absolute right-1 top-1">
          <NotificationDot label={t("notification.activity")} />
        </span>
      )}
    </button>
  );
}
