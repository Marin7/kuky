import { useTranslation } from "react-i18next";
import type { ActivitySummary } from "@/lib/learning";
import { ActivityPanel } from "./ActivityPanel";

interface Props {
  activity: ActivitySummary;
  onChanged?: () => void;
}

/** A page activity, always open in the PDF viewer right after its page (which is the instructions). */
export function ActivityPageSlot({ activity, onChanged }: Props) {
  const { t } = useTranslation();
  const title =
    activity.title ??
    t("learning.activities.pageTitle", { page: activity.page });

  return (
    <section
      className="mx-auto w-full max-w-full overflow-hidden rounded-md border border-primary/25 bg-primary/5 shadow-sm"
      data-activity-after-page={activity.page}
      aria-label={title}
    >
      <div className="flex flex-wrap items-center justify-between gap-2 px-3 py-3 sm:px-4">
        <h3 className="min-w-0 truncate font-medium">{title}</h3>
        {/* Activities show no grading to students: no badge once graded. */}
        {activity.status !== "GRADED" && (
          <span
            className={[
              "shrink-0 rounded-full px-2 py-0.5 text-xs font-medium",
              activity.status === "PENDING"
                ? "bg-muted text-muted-foreground"
                : "bg-green-100 text-green-700",
            ].join(" ")}
          >
            {t(`learning.homework.status.${activity.status}`)}
          </span>
        )}
      </div>
      <div className="border-t border-primary/15 bg-background px-3 py-4 sm:px-4">
        <ActivityPanel activityId={activity.id} onChanged={onChanged} />
      </div>
    </section>
  );
}
