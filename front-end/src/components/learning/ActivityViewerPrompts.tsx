import { useEffect, useState } from "react";
import type { ActivitySummary } from "@/lib/learning";
import { PresentationPdfViewer } from "./PresentationPdfViewer";

interface Props {
  presentationId: string;
  fileId: string;
  activities: ActivitySummary[];
  title?: string;
  displayName?: string;
  embedded?: boolean;
  onActivitiesChanged?: () => void;
}

/**
 * PDF viewer where activities are inserted after their trigger page
 * (between that page and the next), as collapsible slots with media + work.
 */
export function ActivityViewerPrompts({
  presentationId,
  fileId,
  activities,
  title,
  displayName,
  embedded,
  onActivitiesChanged,
}: Props) {
  const [localActivities, setLocalActivities] =
    useState<ActivitySummary[]>(activities);

  useEffect(() => {
    setLocalActivities(activities);
  }, [activities]);

  const fileActivities = localActivities.filter(
    (a) => a.triggerFileId === fileId && a.triggerPage != null,
  );

  return (
    <PresentationPdfViewer
      presentationId={presentationId}
      fileId={fileId}
      title={title}
      displayName={displayName}
      embedded={embedded}
      activities={fileActivities}
      onActivityChanged={(activityId) => {
        setLocalActivities((prev) =>
          prev.map((a) => {
            if (a.id !== activityId) return a;
            return {
              ...a,
              status:
                a.format === "EXERCISE"
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
