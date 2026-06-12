import { useTranslation } from "react-i18next";
import type { HomeworkItem, HomeworkType, HomeworkLevel } from "@/lib/learning";
import { Link } from "@tanstack/react-router";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(iso));
}

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

interface HomeworkItemCardProps {
  item: HomeworkItem;
  onOpen: (item: HomeworkItem) => void;
}

export function HomeworkItemCard({ item, onOpen }: HomeworkItemCardProps) {
  const { t } = useTranslation();
  return (
    <Card className="text-sm">
      <CardContent className="pt-4 space-y-2">
        <div className="flex items-start justify-between gap-3">
          <div className="space-y-1">
            <p className="font-medium text-foreground">{item.title}</p>
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
          <div className="flex shrink-0 items-center gap-1.5">
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
          </div>
        </div>

        <p className="text-muted-foreground">{item.instructions}</p>

        {item.dueOn && (
          <p className="text-xs text-muted-foreground">
            {t("learning.homework.dueOn")}{" "}
            <span className="capitalize">{formatDate(item.dueOn)}</span>
          </p>
        )}

        {item.response && (
          <p className="rounded-md bg-secondary/40 p-2 text-muted-foreground">
            <span className="font-medium text-foreground">
              {t("learning.homework.yourResponse")}{" "}
            </span>
            {item.response}
          </p>
        )}

        {item.format === "EXERCISE" ? (
          <Button
            asChild
            variant={item.status === "GRADED" ? "outline" : "default"}
            size="sm"
            className="h-8 text-xs"
          >
            <Link
              to="/aprendizaje/tarea/$homeworkId"
              params={{ homeworkId: item.id }}
            >
              {item.status === "GRADED"
                ? t("learning.homework.viewResult")
                : t("learning.homework.startExercise")}
            </Link>
          </Button>
        ) : (
          item.status !== "REVIEWED" && (
            <Button
              variant={item.status === "PENDING" ? "default" : "outline"}
              size="sm"
              onClick={() => onOpen(item)}
              className="h-8 text-xs"
            >
              {item.status === "PENDING"
                ? t("learning.homework.submitHomework")
                : t("learning.homework.editResponse")}
            </Button>
          )
        )}
      </CardContent>
    </Card>
  );
}
