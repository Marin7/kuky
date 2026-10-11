import {
  isPresentationPdf,
  type ActivitySummary,
  type UnitQuizSummary,
} from "@/lib/learning";
import { activityAnchorId, pageAnchorId } from "@/lib/unitAnchors";
import type { UnitGroup } from "./unitGroups";

/** One line of a unit's contents panel, in reading order. */
export type OutlineEntry =
  | {
      kind: "page";
      page: number;
      title: string;
      anchorId: string;
      /** The page's activity: the page is then marked as one (a single entry). */
      activity?: {
        id: string;
        status: ActivitySummary["status"];
        anchorId: string;
      };
    }
  | {
      kind: "quiz";
      quizId: string;
      title: string;
      status: UnitQuizSummary["status"];
      unseen: boolean;
    };

export interface UnitOutline {
  entries: OutlineEntry[];
  /** Anchor ids of the page entries, in reading order. */
  anchorIds: string[];
}

/**
 * Pages of the unit's PDF in order (a page with an activity is one entry, marked as an
 * activity), then the quizzes that end the unit in the teacher's order.
 */
export function buildUnitOutline(group: UnitGroup): UnitOutline {
  const entries: OutlineEntry[] = [];
  const pdfPresentation = group.presentations.find((p) =>
    p.files.some((f) => isPresentationPdf(f.contentType)),
  );

  if (pdfPresentation) {
    const activitiesByPage = new Map(
      (pdfPresentation.activities ?? []).map((a) => [a.page, a]),
    );
    (pdfPresentation.pageTitles ?? []).forEach((title, i) => {
      const page = i + 1;
      const activity = activitiesByPage.get(page);
      entries.push({
        kind: "page",
        page,
        title,
        anchorId: pageAnchorId(page),
        activity: activity && {
          id: activity.id,
          status: activity.status,
          anchorId: activityAnchorId(activity.id),
        },
      });
    });
  }

  for (const q of group.quizzes) {
    entries.push({
      kind: "quiz",
      quizId: q.id,
      title: q.title,
      status: q.status,
      unseen: q.unseen,
    });
  }

  const anchorIds = entries.flatMap((e) =>
    e.kind === "quiz" ? [] : [e.anchorId],
  );
  return { entries, anchorIds };
}

/** The student's units of the same level as `current`, in the teacher's order. */
export function levelSiblings(
  groups: UnitGroup[],
  current: UnitGroup,
): UnitGroup[] {
  return groups.filter((g) => g.unitId !== null && g.level === current.level);
}
