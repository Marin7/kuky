import { useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import type { HomeworkItem, SharedPresentationSummary } from "@/lib/learning";
import { downloadPresentationFile, isPresentationPdf } from "@/lib/learning";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { HomeworkItemCard } from "./HomeworkItemCard";

const STATUS_ORDER: Record<string, number> = {
  PENDING: 0,
  SUBMITTED: 1,
  REVIEWED: 2,
  GRADED: 3,
};

function PresentationDownloadCard({
  presentation,
}: {
  presentation: SharedPresentationSummary;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = async (fileId: string, displayName: string) => {
    setDownloadingId(fileId);
    setError(null);
    try {
      await downloadPresentationFile(presentation.id, fileId, displayName);
    } catch {
      setError(t("learning.presentations.loadError"));
    } finally {
      setDownloadingId(null);
    }
  };

  const handleOpen = (fileId: string) => {
    navigate({
      to: "/aprendizaje/presentacion/$presentationId/archivo/$fileId",
      params: {
        presentationId: presentation.id,
        fileId,
      },
    });
  };

  return (
    <Card>
      <CardContent className="space-y-2 pt-4">
        <p className="truncate font-medium">{presentation.title}</p>
        {presentation.files.length > 0 ? (
          <ul className="space-y-1">
            {presentation.files.map((f) => {
              const canView = isPresentationPdf(f.contentType);
              return (
                <li
                  key={f.id}
                  className="flex items-center justify-between gap-2"
                >
                  <span className="truncate text-sm text-muted-foreground">
                    {f.displayName}
                  </span>
                  <div className="flex shrink-0 items-center gap-1.5">
                    {canView && (
                      <Button
                        variant="default"
                        size="sm"
                        onClick={() => handleOpen(f.id)}
                      >
                        {t("learning.presentations.open")}
                      </Button>
                    )}
                    <Button
                      variant={canView ? "outline" : "default"}
                      size="sm"
                      disabled={downloadingId === f.id}
                      onClick={() => handleDownload(f.id, f.displayName)}
                    >
                      {downloadingId === f.id
                        ? t("learning.presentations.downloading")
                        : t("learning.presentations.download")}
                    </Button>
                  </div>
                </li>
              );
            })}
          </ul>
        ) : (
          <span className="text-xs text-muted-foreground">
            {t("learning.presentations.noFile")}
          </span>
        )}
        {error && <p className="text-sm text-destructive">{error}</p>}
      </CardContent>
    </Card>
  );
}

interface Props {
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
  onOpenHomework: (item: HomeworkItem) => void;
  onViewResult: (item: HomeworkItem) => void;
}

export function UnitDetailContent({
  presentations,
  homework,
  onOpenHomework,
  onViewResult,
}: Props) {
  const { t } = useTranslation();

  if (presentations.length === 0 && homework.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("learning.units.unitEmpty")}
      </p>
    );
  }

  return (
    <div className="space-y-8">
      {presentations.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {t("learning.units.presentations")}
          </p>
          <div className="grid gap-3 sm:grid-cols-2">
            {presentations.map((p) => (
              <PresentationDownloadCard key={p.id} presentation={p} />
            ))}
          </div>
        </div>
      )}

      {homework.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {t("learning.units.homework")}
          </p>
          <div className="space-y-3">
            {[...homework]
              .sort(
                (a, b) =>
                  (STATUS_ORDER[a.status] ?? 9) - (STATUS_ORDER[b.status] ?? 9),
              )
              .map((item) => (
                <HomeworkItemCard
                  key={item.id}
                  item={item}
                  onOpen={onOpenHomework}
                  onViewResult={onViewResult}
                />
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
