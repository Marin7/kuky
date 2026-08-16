import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { ChevronDown } from "lucide-react";
import {
  getHomeworkById,
  type HomeworkAdminItem,
  type HomeworkType,
  type HomeworkLevel,
} from "@/lib/admin";
import { homeworkLabels } from "@/lib/homeworkLabels";
import { HomeworkAssigneeList } from "@/components/admin/homework/HomeworkAssigneeList";
import { HomeworkLabelField } from "@/components/admin/homework/HomeworkLabelField";
import { HomeworkPreview } from "@/components/admin/homework/HomeworkPreview";
import { HomeworkReviewDialog } from "@/components/admin/homework/HomeworkReviewDialog";
import { ExerciseResultDialog } from "@/components/admin/homework/ExerciseResultDialog";
import { NotificationDot } from "@/components/NotificationDot";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { notifyBadgesChanged } from "@/lib/notifications";
import { cn } from "@/lib/utils";

const EXPAND_MS = 400;

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${iso}T00:00:00`));
}

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

interface Props {
  item: HomeworkAdminItem;
  labelOptions: string[];
  labelSaving: boolean;
  onPersistLabels: (item: HomeworkAdminItem, next: string[]) => void;
  onAssign: (item: HomeworkAdminItem) => void;
  onEdit: (item: HomeworkAdminItem) => void;
  onDelete: (item: HomeworkAdminItem) => void;
  onUpdated: (item: HomeworkAdminItem) => void;
}

export function HomeworkAdminCard({
  item,
  labelOptions,
  labelSaving,
  onPersistLabels,
  onAssign,
  onEdit,
  onDelete,
  onUpdated,
}: Props) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);
  const [wide, setWide] = useState(false);
  const [openSubmissionId, setOpenSubmissionId] = useState<string | null>(null);
  const [openResultId, setOpenResultId] = useState<string | null>(null);
  const collapseTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (collapseTimer.current) clearTimeout(collapseTimer.current);
    },
    [],
  );

  const onOpenChange = (open: boolean) => {
    setExpanded(open);
    if (open) {
      if (collapseTimer.current) clearTimeout(collapseTimer.current);
      setWide(true);
    } else {
      collapseTimer.current = setTimeout(() => setWide(false), EXPAND_MS);
    }
  };

  const refresh = () => {
    getHomeworkById(item.id)
      .then((updated) => {
        onUpdated(updated);
        notifyBadgesChanged();
      })
      .catch(() => {});
  };

  return (
    <>
      <div className={wide ? "col-span-full" : undefined}>
        <Collapsible open={expanded} onOpenChange={onOpenChange}>
          <Card className="relative flex flex-col gap-0 py-3">
            <CardHeader
              className={cn("px-3 pb-1.5 pt-0", expanded && "pr-10")}
            >
              <div className="flex flex-col gap-1.5">
                <CardTitle className="inline-flex min-w-0 items-center gap-1.5 text-sm leading-snug">
                  {item.title}
                  {item.hasUnseenSubmissions && (
                    <NotificationDot label={t("notification.item")} />
                  )}
                </CardTitle>
                <div className="flex flex-wrap items-center gap-1">
                  {item.homeworkType && (
                    <span
                      className={[
                        "rounded-full px-2 py-0.5 text-xs font-medium",
                        TYPE_CLASS[item.homeworkType],
                      ].join(" ")}
                    >
                      {t(`admin.homework.type.${item.homeworkType}`)}
                    </span>
                  )}
                  {item.level && (
                    <span
                      className={[
                        "rounded-full px-2 py-0.5 text-xs font-medium",
                        LEVEL_CLASS[item.level],
                      ].join(" ")}
                    >
                      {item.level}
                    </span>
                  )}
                  {(item.format === "EXERCISE" ||
                    item.composition === "ALL_AUTO") && (
                    <span className="rounded-full bg-pink-100 px-2 py-0.5 text-xs font-medium text-pink-700">
                      {t("admin.homework.exercise")}
                    </span>
                  )}
                  {(item.format === "MIXED" ||
                    item.composition === "MIXED") && (
                    <span className="rounded-full bg-violet-100 px-2 py-0.5 text-xs font-medium text-violet-700">
                      {t("admin.homework.mixed")}
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => onAssign(item)}
                  >
                    {t("admin.homework.assign")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => onEdit(item)}
                  >
                    {t("admin.homework.edit")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs text-destructive"
                    onClick={() => onDelete(item)}
                  >
                    {t("admin.homework.delete")}
                  </Button>
                </div>
                <div className="flex items-start gap-1">
                  <div className="min-w-0 flex-1">
                    <HomeworkLabelField
                      compact
                      value={homeworkLabels(item)}
                      existing={labelOptions}
                      disabled={labelSaving}
                      onChange={(next) => onPersistLabels(item, next)}
                    />
                  </div>
                  {!expanded && (
                    <CollapsibleTrigger asChild>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="-mr-1 h-7 w-7 shrink-0 text-muted-foreground"
                        aria-label={t("admin.homework.details")}
                      >
                        <ChevronDown
                          className="h-4 w-4 -rotate-45"
                          aria-hidden
                        />
                      </Button>
                    </CollapsibleTrigger>
                  )}
                </div>
                {item.dueOn && (
                  <p className="text-xs text-muted-foreground">
                    {t("admin.homework.dueOn")} {formatDate(item.dueOn)}
                  </p>
                )}
              </div>
            </CardHeader>
            <CollapsibleContent className="overflow-hidden data-[state=closed]:animate-homework-collapse data-[state=open]:animate-homework-expand">
              <CardContent className="space-y-4 border-t px-3 pt-3 text-sm">
                <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_18rem]">
                  <HomeworkPreview item={item} />
                  <div className="space-y-1.5">
                    <p className="text-xs font-medium">
                      {t("admin.homework.assignedTo")}
                    </p>
                    {item.assignees.length === 0 ? (
                      <p className="text-xs text-muted-foreground">
                        {t("admin.homework.unassigned")}
                      </p>
                    ) : (
                      <HomeworkAssigneeList
                        assignees={item.assignees}
                        onOpenResult={setOpenResultId}
                        onOpenReview={setOpenSubmissionId}
                      />
                    )}
                  </div>
                </div>
              </CardContent>
            </CollapsibleContent>
            {expanded && (
              <CollapsibleTrigger asChild>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute top-2 right-2 z-10 h-7 w-7 text-muted-foreground"
                  aria-label={t("admin.homework.details")}
                >
                  <ChevronDown
                    className="h-4 w-4 -rotate-[135deg]"
                    aria-hidden
                  />
                </Button>
              </CollapsibleTrigger>
            )}
          </Card>
        </Collapsible>
      </div>
      {openSubmissionId && (
        <HomeworkReviewDialog
          submissionId={openSubmissionId}
          onClose={() => setOpenSubmissionId(null)}
          onReviewed={() => {
            setOpenSubmissionId(null);
            refresh();
          }}
        />
      )}
      {openResultId && (
        <ExerciseResultDialog
          submissionId={openResultId}
          onClose={() => {
            setOpenResultId(null);
            refresh();
          }}
          onFeedbackSaved={refresh}
        />
      )}
    </>
  );
}
