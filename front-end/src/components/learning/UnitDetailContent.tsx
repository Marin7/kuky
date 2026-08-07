import { useState } from "react";
import { useTranslation } from "react-i18next";
import type {
  HomeworkItem,
  HomeworkLevel,
  HomeworkType,
  SharedPresentationSummary,
} from "@/lib/learning";
import { downloadPresentationFile, isPresentationPdf } from "@/lib/learning";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Button } from "@/components/ui/button";
import { ActivityViewerPrompts } from "./ActivityViewerPrompts";
import { HomeworkInlinePanel } from "./HomeworkInlinePanel";

type UnitListItem =
  | { kind: "presentation"; position: number; presentation: SharedPresentationSummary }
  | { kind: "homework"; position: number; homework: HomeworkItem };

const STATUS_CLASS: Record<HomeworkItem["status"], string> = {
  PENDING: "bg-muted text-muted-foreground",
  SUBMITTED: "bg-green-100 text-green-700",
  REVIEWED: "bg-blue-100 text-blue-700",
  GRADED: "bg-green-100 text-green-700",
};

const TYPE_CLASS: Record<HomeworkType, string> = {
  AUDIO: "bg-purple-100 text-purple-700",
  READ: "bg-blue-100 text-blue-700",
  WRITE: "bg-yellow-100 text-yellow-700",
  GRAMMAR: "bg-orange-100 text-orange-700",
};

const LEVEL_CLASS: Record<HomeworkLevel, string> = {
  A1: "bg-green-100 text-green-700",
  A2: "bg-green-100 text-green-700",
  B1: "bg-teal-100 text-teal-700",
  B2: "bg-teal-100 text-teal-700",
  C1: "bg-indigo-100 text-indigo-700",
  C2: "bg-indigo-100 text-indigo-700",
};

function PresentationExpandBody({
  presentation,
  onActivitiesChanged,
}: {
  presentation: SharedPresentationSummary;
  onActivitiesChanged: () => void;
}) {
  const { t } = useTranslation();
  const pdfFiles = presentation.files.filter((f) =>
    isPresentationPdf(f.contentType),
  );
  const [viewingFileId, setViewingFileId] = useState<string | null>(
    () => pdfFiles[0]?.id ?? null,
  );
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const activities = presentation.activities ?? [];

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

  if (presentation.files.length === 0) {
    return (
      <p className="text-xs text-muted-foreground">
        {t("learning.presentations.noFile")}
      </p>
    );
  }

  const viewingFile = presentation.files.find((f) => f.id === viewingFileId);

  return (
    <div className="space-y-3">
      <ul className="space-y-1.5">
        {presentation.files.map((f) => {
          const canView = isPresentationPdf(f.contentType);
          const isViewing = viewingFileId === f.id;
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
                    variant={isViewing ? "secondary" : "default"}
                    size="sm"
                    onClick={() => setViewingFileId(f.id)}
                  >
                    {isViewing
                      ? t("learning.units.viewingFile")
                      : t("learning.presentations.open")}
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
      {error && <p className="text-sm text-destructive">{error}</p>}
      {viewingFile && isPresentationPdf(viewingFile.contentType) && (
        <div className="overflow-x-auto rounded-md border bg-background p-2">
          <ActivityViewerPrompts
            presentationId={presentation.id}
            fileId={viewingFile.id}
            activities={activities}
            title={presentation.title}
            displayName={viewingFile.displayName}
            embedded
            onActivitiesChanged={onActivitiesChanged}
          />
        </div>
      )}
    </div>
  );
}

function HomeworkTriggerMeta({ item }: { item: HomeworkItem }) {
  const { t } = useTranslation();
  return (
    <div className="flex min-w-0 flex-1 flex-col gap-1.5 pr-2 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0 space-y-1">
        <p className="truncate font-medium text-foreground">{item.title}</p>
        <div className="flex flex-wrap items-center gap-1">
          {item.homeworkType && (
            <span
              className={[
                "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                TYPE_CLASS[item.homeworkType],
              ].join(" ")}
            >
              {t(`learning.homework.type.${item.homeworkType}`)}
            </span>
          )}
          {item.level && (
            <span
              className={[
                "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                LEVEL_CLASS[item.level],
              ].join(" ")}
            >
              {item.level}
            </span>
          )}
        </div>
      </div>
      <div className="flex shrink-0 flex-wrap items-center gap-1.5">
        {item.overdue && (
          <span className="inline-block rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
            {t("learning.homework.overdue")}
          </span>
        )}
        <span
          className={[
            "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
            STATUS_CLASS[item.status],
          ].join(" ")}
        >
          {t(`learning.homework.status.${item.status}`)}
          {item.status === "GRADED" &&
            item.scorePercent !== null &&
            ` — ${item.scorePercent}%`}
        </span>
        {item.hasTeacherFeedback && (
          <span className="inline-block rounded-full bg-sky-100 px-2 py-0.5 text-xs font-medium text-sky-800">
            {t("learning.homework.hasTeacherFeedback")}
          </span>
        )}
      </div>
    </div>
  );
}

interface Props {
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
  onHomeworkChanged: () => void;
}

export function UnitDetailContent({
  presentations,
  homework,
  onHomeworkChanged,
}: Props) {
  const { t } = useTranslation();
  const [openItem, setOpenItem] = useState<string | undefined>();

  const items: UnitListItem[] = [
    ...presentations.map((p) => ({
      kind: "presentation" as const,
      position: p.unitPosition ?? Number.MAX_SAFE_INTEGER,
      presentation: p,
    })),
    ...homework.map((h) => ({
      kind: "homework" as const,
      position: h.unitPosition ?? Number.MAX_SAFE_INTEGER,
      homework: h,
    })),
  ].sort((a, b) => a.position - b.position);

  if (items.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("learning.units.unitEmpty")}
      </p>
    );
  }

  return (
    <Accordion
      type="single"
      collapsible
      value={openItem}
      onValueChange={(v) => setOpenItem(v || undefined)}
      className="w-full space-y-2"
    >
      {items.map((item) =>
        item.kind === "presentation" ? (
          <AccordionItem
            key={`p-${item.presentation.id}`}
            value={`p-${item.presentation.id}`}
            className="rounded-lg border border-border bg-card px-4"
          >
            <AccordionTrigger className="hover:no-underline">
              <div className="min-w-0 flex-1 space-y-0.5 text-left">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                  {t("learning.units.itemPresentation")}
                </p>
                <p className="truncate font-medium">{item.presentation.title}</p>
              </div>
            </AccordionTrigger>
            <AccordionContent className="overflow-visible">
              <PresentationExpandBody
                presentation={item.presentation}
                onActivitiesChanged={onHomeworkChanged}
              />
            </AccordionContent>
          </AccordionItem>
        ) : (
          <AccordionItem
            key={`h-${item.homework.id}`}
            value={`h-${item.homework.id}`}
            className="rounded-lg border border-border bg-card px-4"
          >
            <AccordionTrigger className="hover:no-underline">
              <div className="min-w-0 flex-1 space-y-0.5 text-left">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                  {t("learning.units.itemHomework")}
                </p>
                <HomeworkTriggerMeta item={item.homework} />
              </div>
            </AccordionTrigger>
            <AccordionContent className="overflow-visible">
              <HomeworkInlinePanel
                item={item.homework}
                onChanged={onHomeworkChanged}
              />
            </AccordionContent>
          </AccordionItem>
        ),
      )}
    </Accordion>
  );
}
