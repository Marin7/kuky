import { useState } from "react";
import { useTranslation } from "react-i18next";
import { ChevronDown } from "lucide-react";
import type { ActivitySummary } from "@/lib/learning";
import { extractYoutubeVideoId, youtubeEmbedUrl, activityImageUrl } from "@/lib/youtube";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Button } from "@/components/ui/button";
import { ActivityPanel } from "./ActivityPanel";

interface Props {
  activity: ActivitySummary;
  onChanged?: () => void;
}

export function ActivityPageSlot({ activity, onChanged }: Props) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const videoId = extractYoutubeVideoId(activity.youtubeUrl);

  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className="mx-auto w-full max-w-full overflow-hidden rounded-md border border-primary/25 bg-primary/5 shadow-sm"
      data-activity-after-page={activity.triggerPage ?? undefined}
    >
      <div className="flex flex-wrap items-center justify-between gap-2 px-3 py-3 sm:px-4">
        <CollapsibleTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            className="h-auto flex-1 justify-between gap-2 px-2 py-2 text-left font-medium hover:bg-primary/10"
          >
            <span className="min-w-0 truncate">
              {open
                ? t("learning.activities.hideActivity", {
                    title: activity.title,
                  })
                : t("learning.activities.viewActivity", {
                    title: activity.title,
                  })}
            </span>
            <ChevronDown
              className={[
                "h-4 w-4 shrink-0 text-muted-foreground transition-transform",
                open ? "rotate-180" : "",
              ].join(" ")}
              aria-hidden
            />
          </Button>
        </CollapsibleTrigger>
        <span
          className={[
            "shrink-0 rounded-full px-2 py-0.5 text-xs font-medium",
            activity.status === "PENDING"
              ? "bg-muted text-muted-foreground"
              : "bg-green-100 text-green-700",
          ].join(" ")}
        >
          {t(`learning.homework.status.${activity.status}`)}
          {activity.status === "GRADED" &&
            activity.scorePercent != null &&
            ` — ${activity.scorePercent}%`}
        </span>
      </div>
      <CollapsibleContent>
        <div className="space-y-4 border-t border-primary/15 bg-background px-3 py-4 sm:px-4">
          {videoId && (
            <div className="aspect-video w-full overflow-hidden rounded-md border bg-black">
              <iframe
                title={activity.title}
                src={youtubeEmbedUrl(videoId)}
                className="h-full w-full"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
              />
            </div>
          )}
          {activity.imageId && (
            <img
              src={activityImageUrl(activity.imageId)}
              alt=""
              className="max-h-[28rem] w-full rounded-md border object-contain bg-muted/20"
            />
          )}
          {activity.instructionsText?.trim() && (
            <div className="space-y-1">
              <p className="text-sm font-medium">
                {t("learning.activities.instructions")}
              </p>
              <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground">
                {activity.instructionsText}
              </p>
            </div>
          )}
          <ActivityPanel
            activityId={activity.id}
            compact
            onChanged={onChanged}
          />
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}
