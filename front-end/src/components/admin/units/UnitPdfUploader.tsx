import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { uploadUnitPdf, type ApiError, type UnitDetail } from "@/lib/admin";
import { countPdfPages, isPdfFile } from "@/lib/pdf";
import { Button } from "@/components/ui/button";

interface Props {
  unit: UnitDetail;
  onUploaded: (detail: UnitDetail) => void;
}

/**
 * Picks a PDF, counts its pages in the browser, and — when replacing with fewer pages than
 * the marked activities need — asks the teacher before those activities are deleted.
 */
export function UnitPdfUploader({ unit, onUploaded }: Props) {
  const { t } = useTranslation();
  const inputRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFile = async (file: File) => {
    setError(null);
    if (!isPdfFile(file)) {
      setError(t("admin.units.editor.pdfOnly"));
      return;
    }
    setBusy(true);
    try {
      let pageCount: number;
      try {
        pageCount = await countPdfPages(file);
      } catch {
        setError(t("admin.units.editor.pdfUnreadable"));
        return;
      }
      const removed = unit.activities.filter((a) => a.page > pageCount);
      if (
        removed.length > 0 &&
        !window.confirm(
          t("admin.units.editor.confirmRemoveActivities", {
            count: removed.length,
            pages: pageCount,
            list: removed.map((a) => a.page).join(", "),
          }),
        )
      ) {
        return;
      }
      const detail = await uploadUnitPdf(
        unit.id,
        file,
        pageCount,
        removed.length > 0,
      );
      onUploaded(detail);
    } catch (err) {
      setError(
        (err as ApiError).message ?? t("admin.units.editor.uploadError"),
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      <input
        ref={inputRef}
        type="file"
        accept="application/pdf,.pdf"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          e.target.value = "";
          if (file) void handleFile(file);
        }}
      />
      <Button
        size="sm"
        variant={unit.pdf ? "outline" : "default"}
        disabled={busy}
        onClick={() => inputRef.current?.click()}
      >
        {busy
          ? t("admin.units.editor.uploading")
          : unit.pdf
            ? t("admin.units.editor.replacePdf")
            : t("admin.units.editor.uploadPdf")}
      </Button>
      {unit.pdf && (
        <span className="text-xs text-muted-foreground">
          {t("admin.units.editor.pdfMeta", {
            name: unit.pdf.originalName,
            pages: unit.pdf.pageCount,
          })}
        </span>
      )}
      {error && <p className="w-full text-xs text-destructive">{error}</p>}
    </div>
  );
}
