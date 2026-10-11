import { useEffect, useMemo, useRef } from "react";
import { Link } from "@tanstack/react-router";
import {
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Circle,
  ClipboardCheck,
  Lock,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { NotificationDot } from "@/components/NotificationDot";
import { cn } from "@/lib/utils";
import type { UnitGroup } from "./unitGroups";
import {
  buildUnitOutline,
  levelSiblings,
  type OutlineEntry,
} from "./unitOutline";

/** Set (as a literal attribute) on the element that scrolls the panel. */
const UNIT_CONTENTS_SCROLL_ATTR = "data-unit-contents-scroll";

interface Props {
  groups: UnitGroup[];
  /** The open unit (never the "Otros" bucket). */
  current: UnitGroup;
  activeAnchorId: string | null;
  /** The end quiz shown in place of the PDF; its unit's pages are then collapsed. */
  openQuizId: string | null;
  /** Pages after this one are locked until a pending activity is submitted. */
  lastUnlockedPage: number;
  /** The unit's end quizzes are locked until every activity is submitted. */
  quizzesLocked: boolean;
  /** Page/activity selected: show `page` and bring the anchor into view. */
  onSelectAnchor: (anchorId: string, page: number) => void;
  /** A link (another unit or a quiz) was followed. */
  onNavigate?: () => void;
}

const entryBase =
  "flex w-full min-w-0 items-center gap-2 rounded-md px-2 py-0.5 text-left transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";

function OutlineItem({
  unitId,
  entry,
  active,
  lastUnlockedPage,
  quizzesLocked,
  onSelectAnchor,
  onNavigate,
}: {
  unitId: string;
  entry: OutlineEntry;
  lastUnlockedPage: number;
  quizzesLocked: boolean;
  active: boolean;
  onSelectAnchor: (anchorId: string, page: number) => void;
  onNavigate?: () => void;
}) {
  const { t } = useTranslation();
  const activeClass = active && "bg-primary/10 font-medium text-primary";

  if (entry.kind === "quiz" && quizzesLocked) {
    // Locked until every activity of the unit is submitted.
    return (
      <span
        className={cn(
          entryBase,
          "cursor-not-allowed text-muted-foreground/70 hover:bg-transparent",
        )}
        title={t("learning.units.lockedQuiz")}
        aria-disabled="true"
      >
        <ClipboardCheck className="h-4 w-4 shrink-0" aria-hidden />
        <span className="sr-only">{t("learning.units.quizLabel")}:</span>
        <span className="min-w-0 flex-1 truncate">{entry.title}</span>
        <Lock className="h-3.5 w-3.5 shrink-0" aria-hidden />
        <span className="sr-only">{t("learning.units.lockedQuiz")}</span>
      </span>
    );
  }

  if (entry.kind === "quiz") {
    // The quiz opens inside the unit page, in place of the PDF.
    return (
      <Link
        to="/aprendizaje/unidad/$unitId"
        params={{ unitId }}
        search={(prev) => ({ ...prev, quiz: entry.quizId })}
        onClick={onNavigate}
        aria-current={active ? "page" : undefined}
        className={cn(entryBase, "text-foreground", activeClass)}
        title={entry.title}
      >
        <ClipboardCheck className="h-4 w-4 shrink-0 text-primary" aria-hidden />
        <span className="sr-only">{t("learning.units.quizLabel")}:</span>
        <span className="min-w-0 flex-1 truncate">{entry.title}</span>
        <span className="shrink-0 text-xs text-muted-foreground">
          {t(`quiz.status.${entry.status}`)}
        </span>
        {entry.unseen && <NotificationDot label={t("notification.item")} />}
      </Link>
    );
  }

  const activity = entry.activity;
  const done = activity !== undefined && activity.status !== "PENDING";
  const Icon = done ? CheckCircle2 : Circle;
  const locked = entry.page > lastUnlockedPage;
  return (
    <button
      type="button"
      data-anchor={entry.anchorId}
      aria-current={active ? "location" : undefined}
      disabled={locked}
      onClick={() => onSelectAnchor(entry.anchorId, entry.page)}
      className={cn(
        entryBase,
        "text-foreground/90",
        activeClass,
        locked &&
          "cursor-not-allowed text-muted-foreground/70 hover:bg-transparent",
      )}
      title={
        locked
          ? t("learning.units.lockedPage", { page: lastUnlockedPage })
          : entry.title
      }
    >
      <span className="w-4 shrink-0 text-right text-xs tabular-nums text-muted-foreground">
        {entry.page}
      </span>
      <span className="min-w-0 flex-1 truncate">{entry.title}</span>
      {locked && <Lock className="h-3.5 w-3.5 shrink-0" aria-hidden />}
      {/* A page with an activity is one entry, marked with the activity's status. */}
      {activity && (
        <>
          <span className="sr-only">
            · {t("learning.units.activityLabel")}:{" "}
            {t(`learning.homework.status.${activity.status}`)}
          </span>
          <Icon
            className={cn(
              "h-4 w-4 shrink-0",
              done ? "text-green-600" : "text-primary",
            )}
            aria-hidden
          />
        </>
      )}
    </button>
  );
}

/**
 * Left panel of a unit page: the student's units of the same level, the open one
 * expanded into its pages, activities and end quizzes.
 */
export function UnitContentsPanel({
  groups,
  current,
  activeAnchorId,
  openQuizId,
  lastUnlockedPage,
  quizzesLocked,
  onSelectAnchor,
  onNavigate,
}: Props) {
  const { t } = useTranslation();
  const navRef = useRef<HTMLElement>(null);
  const outline = useMemo(() => buildUnitOutline(current), [current]);
  const siblings = levelSiblings(groups, current);
  const hasPages = outline.entries.some((e) => e.kind === "page");
  const quizOpen = openQuizId !== null;
  // With a quiz open the unit is collapsed to its quizzes.
  const entries = quizOpen
    ? outline.entries.filter((e) => e.kind === "quiz")
    : outline.entries;

  // Keep the highlighted entry visible inside the panel (never scroll the window).
  useEffect(() => {
    const nav = navRef.current;
    if (!nav || !activeAnchorId) return;
    const container = nav.closest<HTMLElement>(
      `[${UNIT_CONTENTS_SCROLL_ATTR}]`,
    );
    const item = nav.querySelector<HTMLElement>(
      `[data-anchor="${activeAnchorId}"]`,
    );
    if (!container || !item) return;
    const box = container.getBoundingClientRect();
    const rect = item.getBoundingClientRect();
    const margin = 24;
    if (rect.top < box.top + margin) {
      container.scrollTop -= box.top + margin - rect.top;
    } else if (rect.bottom > box.bottom - margin) {
      container.scrollTop += rect.bottom - (box.bottom - margin);
    }
  }, [activeAnchorId]);

  return (
    <nav
      ref={navRef}
      aria-label={t("learning.units.contents")}
      className="text-sm"
    >
      <p className="px-2 pb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        {t("learning.units.levelHeading", { level: current.level ?? "" })}
      </p>
      <ul className="space-y-1">
        {siblings.map((g) =>
          g.unitId === current.unitId ? (
            <li key={g.key}>
              {quizOpen ? (
                // Collapsed: the unit name goes back to its pages.
                <Link
                  to="/aprendizaje/unidad/$unitId"
                  params={{ unitId: current.unitId as string }}
                  search={(prev) => ({ page: prev.page })}
                  onClick={onNavigate}
                  title={t("learning.units.backToUnit")}
                  className={cn(entryBase, "bg-muted py-1.5 font-semibold")}
                >
                  <ChevronRight
                    className="h-4 w-4 shrink-0 text-muted-foreground"
                    aria-hidden
                  />
                  <span className="min-w-0 flex-1 truncate">{g.subject}</span>
                </Link>
              ) : (
                <p
                  aria-current="page"
                  className="flex items-center gap-2 rounded-md bg-muted px-2 py-1.5 font-semibold text-foreground"
                >
                  <ChevronDown
                    className="h-4 w-4 shrink-0 text-muted-foreground"
                    aria-hidden
                  />
                  <span className="min-w-0 flex-1 truncate">{g.subject}</span>
                </p>
              )}
              <ul className="mt-1 ml-1 border-l border-border pl-0.5">
                {!hasPages && !quizOpen && (
                  <li className="px-2 py-1.5 text-muted-foreground">
                    {t("learning.units.noPages")}
                  </li>
                )}
                {entries.map((entry) => (
                  <li
                    key={
                      entry.kind === "quiz"
                        ? `quiz-${entry.quizId}`
                        : entry.anchorId
                    }
                  >
                    <OutlineItem
                      unitId={current.unitId as string}
                      lastUnlockedPage={lastUnlockedPage}
                      quizzesLocked={quizzesLocked}
                      entry={entry}
                      active={
                        entry.kind === "quiz"
                          ? entry.quizId === openQuizId
                          : entry.anchorId === activeAnchorId
                      }
                      onSelectAnchor={onSelectAnchor}
                      onNavigate={onNavigate}
                    />
                  </li>
                ))}
              </ul>
            </li>
          ) : (
            <li key={g.key}>
              <Link
                to="/aprendizaje/unidad/$unitId"
                params={{ unitId: g.unitId as string }}
                onClick={onNavigate}
                className={cn(
                  entryBase,
                  "py-1.5 text-muted-foreground hover:text-foreground",
                )}
              >
                <span className="min-w-0 flex-1 truncate">{g.subject}</span>
                {g.unseen && <NotificationDot label={t("notification.item")} />}
              </Link>
            </li>
          ),
        )}
      </ul>
    </nav>
  );
}
