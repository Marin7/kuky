import { ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import type {
  MistakePeriod,
  StudentMistakeEntry,
  StudentMistakes,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
} from "@/components/ui/pagination";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { QuestionCard } from "@/components/learning/QuestionHeading";
import { MistakeOnlyDetail } from "./MistakeOnlyDetail";

const ALL_LABELS = "ALL";

const PERIODS: { value: MistakePeriod; key: string }[] = [
  { value: "ALL", key: "periodAll" },
  { value: "DAYS_30", key: "period30" },
  { value: "DAYS_90", key: "period90" },
];

interface Props {
  data: StudentMistakes | null;
  loading: boolean;
  error: string | null;
  period: MistakePeriod;
  labelKey: string | null;
  onPeriodChange: (period: MistakePeriod) => void;
  onLabelChange: (labelKey: string | null) => void;
  onPageChange: (page: number) => void;
  onOpen: (entry: StudentMistakeEntry) => void;
}

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(iso));
}

/** Expanded "Errores frecuentes": filters, summary, one page of mistakes. */
export function StudentMistakesList({
  data,
  loading,
  error,
  period,
  labelKey,
  onPeriodChange,
  onLabelChange,
  onPageChange,
  onOpen,
}: Props) {
  const { t } = useTranslation();

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <Select
          value={period}
          onValueChange={(v) => onPeriodChange(v as MistakePeriod)}
        >
          <SelectTrigger
            className="h-8 w-44 text-xs"
            aria-label={t("admin.studentProfile.mistakes.periodFilter")}
          >
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {PERIODS.map((p) => (
              <SelectItem key={p.value} value={p.value}>
                {t(`admin.studentProfile.mistakes.${p.key}` as never)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {data && data.labelOptions.length > 0 && (
          <Select
            value={labelKey ?? ALL_LABELS}
            onValueChange={(v) => onLabelChange(v === ALL_LABELS ? null : v)}
          >
            <SelectTrigger
              className="h-8 w-48 text-xs"
              aria-label={t("admin.studentProfile.mistakes.labelFilter")}
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL_LABELS}>
                {t("admin.studentProfile.mistakes.allLabels")}
              </SelectItem>
              {data.labelOptions.map((o) => (
                <SelectItem key={o.key} value={o.key}>
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}
      {!data && loading && (
        <p className="text-sm text-muted-foreground animate-pulse">
          {t("admin.studentProfile.loading")}
        </p>
      )}

      {data && (
        <div
          className={`space-y-3 transition-opacity ${loading ? "opacity-60" : ""}`}
        >
          {data.summary.answeredCount === 0 ? (
            <p className="text-sm text-muted-foreground">
              {period === "ALL" && labelKey === null
                ? t("admin.studentProfile.mistakes.emptyNone")
                : t("admin.studentProfile.mistakes.emptyFiltered")}
            </p>
          ) : data.summary.mistakeCount === 0 ? (
            <p className="text-sm text-muted-foreground">
              {t("admin.studentProfile.mistakes.emptyNoMistakes", {
                answered: data.summary.answeredCount,
              })}
            </p>
          ) : (
            <>
              <p className="text-sm text-muted-foreground">
                {t("admin.studentProfile.mistakes.summary", {
                  mistakes: data.summary.mistakeCount,
                  answered: data.summary.answeredCount,
                })}
              </p>
              {data.entries.map((entry) => (
                <MistakeEntryCard
                  key={`${entry.submissionId}-${entry.question.id}`}
                  entry={entry}
                  onOpen={onOpen}
                />
              ))}
              {data.totalPages > 1 && (
                <MistakesPagination
                  page={data.page}
                  totalPages={data.totalPages}
                  onPageChange={onPageChange}
                />
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

function MistakeEntryCard({
  entry,
  onOpen,
}: {
  entry: StudentMistakeEntry;
  onOpen: (entry: StudentMistakeEntry) => void;
}) {
  const { t } = useTranslation();
  return (
    <QuestionCard>
      <div className="flex flex-wrap items-start justify-between gap-2 border-b pb-2">
        <div className="min-w-0 space-y-1">
          <p className="text-sm font-medium">{entry.homeworkTitle}</p>
          <p className="text-xs text-muted-foreground">
            {t("admin.studentProfile.mistakes.questionN", {
              n: entry.questionPosition,
            })}
            {" · "}
            {t("admin.studentProfile.mistakes.submittedOn", {
              date: formatDate(entry.submittedAt),
            })}
          </p>
          {entry.labels.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {entry.labels.map((label) => (
                <span
                  key={label}
                  className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground"
                >
                  {label}
                </span>
              ))}
            </div>
          )}
        </div>
        <button
          type="button"
          onClick={() => onOpen(entry)}
          className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary hover:underline"
        >
          {t("admin.studentProfile.mistakes.openHomework")}
        </button>
      </div>
      <MistakeOnlyDetail entry={entry} />
    </QuestionCard>
  );
}

function MistakesPagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col items-center gap-1">
      <Pagination>
        <PaginationContent>
          <PaginationItem>
            <Button
              variant="ghost"
              size="sm"
              className="gap-1 pl-2.5"
              disabled={page <= 1}
              onClick={() => onPageChange(page - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
              <span>{t("admin.studentProfile.mistakes.previous")}</span>
            </Button>
          </PaginationItem>
          <PaginationItem>
            <span className="px-2 text-xs text-muted-foreground">
              {t("admin.studentProfile.mistakes.pageOf", {
                page,
                total: totalPages,
              })}
            </span>
          </PaginationItem>
          <PaginationItem>
            <Button
              variant="ghost"
              size="sm"
              className="gap-1 pr-2.5"
              disabled={page >= totalPages}
              onClick={() => onPageChange(page + 1)}
            >
              <span>{t("admin.studentProfile.mistakes.next")}</span>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </PaginationItem>
        </PaginationContent>
      </Pagination>
    </div>
  );
}
