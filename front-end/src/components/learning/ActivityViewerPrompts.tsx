import { useEffect, useState } from "react";
import { resolveComposition, type ActivitySummary } from "@/lib/learning";
import { PresentationPdfViewer } from "./PresentationPdfViewer";

interface Props {
  presentationId: string;
  fileId: string;
  activities: ActivitySummary[];
  title?: string;
  displayName?: string;
  embedded?: boolean;
  onActivitiesChanged?: () => void;
  /** Single-page mode: only this page (and its activity) is shown. */
  currentPage?: number;
}

/**
 * PDF viewer for a unit PDF: each page activity is a collapsible slot right after its
 * page (the page above is the instructions).
 */
export function ActivityViewerPrompts({
  presentationId,
  fileId,
  activities,
  title,
  displayName,
  embedded,
  onActivitiesChanged,
  currentPage,
}: Props) {
  const [localActivities, setLocalActivities] =
    useState<ActivitySummary[]>(activities);

  useEffect(() => {
    setLocalActivities(activities);
  }, [activities]);

  return (
    <PresentationPdfViewer
      presentationId={presentationId}
      fileId={fileId}
      title={title}
      displayName={displayName}
      embedded={embedded}
      currentPage={currentPage}
      activities={localActivities}
      onActivityChanged={(activityId) => {
        setLocalActivities((prev) =>
          prev.map((a) => {
            if (a.id !== activityId) return a;
            const composition = resolveComposition(a);
            return {
              ...a,
              status:
                composition === "ALL_AUTO"
                  ? ("GRADED" as const)
                  : ("SUBMITTED" as const),
            };
          }),
        );
        onActivitiesChanged?.();
      }}
    />
  );
}
