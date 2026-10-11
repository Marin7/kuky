import { useTranslation } from "react-i18next";
import type { ActivitySummary } from "@/lib/learning";
import { activityAnchorId } from "@/lib/unitAnchors";
import { ActivityPanel } from "./ActivityPanel";

interface Props {
  activity: ActivitySummary;
  onChanged?: () => void;
}

/**
 * A page activity, always open in the PDF viewer right after its page (which is the
 * instructions): just its questions, with no box or header of its own.
 */
export function ActivityPageSlot({ activity, onChanged }: Props) {
  const { t } = useTranslation();
  return (
    <section
      id={activityAnchorId(activity.id)}
      // The shared answer forms start with a top margin; drop it so questions sit under the page.
      className="w-full max-w-full scroll-mt-36 md:scroll-mt-24 [&>div>div:first-child]:mt-0"
      data-activity-after-page={activity.page}
      aria-label={t("learning.activities.label", { page: activity.page })}
    >
      <ActivityPanel activityId={activity.id} onChanged={onChanged} />
    </section>
  );
}
