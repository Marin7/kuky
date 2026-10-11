import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { ChevronLeft, ChevronRight, ListTree, Lock } from "lucide-react";
import { useTranslation } from "react-i18next";
import { markUnitSeen, notifyBadgesChanged } from "@/lib/notifications";
import { pageAnchorId, scrollToUnitAnchor } from "@/lib/unitAnchors";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import {
  getLearning,
  isPresentationPdf,
  type LearningResponse,
  type SharedPresentationSummary,
} from "@/lib/learning";
import { ActivityViewerPrompts } from "./ActivityViewerPrompts";
import { QuizTakeView } from "./QuizTakeView";
import { UnitContentsPanel } from "./UnitContentsPanel";
import { UnitDetailContent } from "./UnitDetailContent";
import { buildGroups, findUnitGroup } from "./unitGroups";
import { buildUnitOutline } from "./unitOutline";
import { useActiveUnitAnchor } from "./useActiveUnitAnchor";

interface Props {
  /** Null selects the unattached "Otros" bucket. */
  unitId: string | null;
  /** One of the unit's end quizzes, shown instead of the PDF. */
  quizId?: string | null;
  /** The PDF page shown (1-based; clamped to the unit's pages). */
  page?: number;
}

function UnitSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-5 w-48" />
      <Skeleton className="h-8 w-64" />
      <div className="grid gap-3 sm:grid-cols-2">
        <Skeleton className="h-24 w-full rounded-lg" />
        <Skeleton className="h-24 w-full rounded-lg" />
      </div>
      <Skeleton className="h-20 w-full rounded-lg" />
    </div>
  );
}

/** A unit's PDF, view-only (units are not downloadable), with its activities after their pages. */
function UnitPdf({
  presentation,
  currentPage,
  onChanged,
}: {
  presentation: SharedPresentationSummary;
  currentPage: number;
  onChanged: () => void;
}) {
  const { t } = useTranslation();
  const pdf = presentation.files.find((f) => isPresentationPdf(f.contentType));
  if (!pdf) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("learning.units.unitEmpty")}
      </p>
    );
  }
  return (
    <div>
      <ActivityViewerPrompts
        presentationId={presentation.id}
        fileId={pdf.id}
        activities={presentation.activities ?? []}
        title={presentation.title}
        embedded
        currentPage={currentPage}
        onActivitiesChanged={onChanged}
      />
    </div>
  );
}

/** Previous / next page arrows with "Página N de M"; next is locked by a pending activity. */
function UnitPager({
  page,
  pageCount,
  lastUnlockedPage,
  onGo,
}: {
  page: number;
  pageCount: number;
  lastUnlockedPage: number;
  onGo: (page: number) => void;
}) {
  const { t } = useTranslation();
  const blocked = page >= lastUnlockedPage && page < pageCount;
  return (
    <nav
      aria-label={t("learning.units.pagination")}
      className="flex shrink-0 items-center gap-3"
    >
      <Button
        variant="outline"
        size="sm"
        disabled={page <= 1}
        onClick={() => onGo(page - 1)}
      >
        <ChevronLeft className="h-4 w-4" aria-hidden />
        {t("learning.units.prevPage")}
      </Button>
      <span className="text-sm tabular-nums text-muted-foreground">
        {t("learning.units.pageOf", { page, count: pageCount })}
      </span>
      <Button
        variant="outline"
        size="sm"
        disabled={page >= lastUnlockedPage}
        title={blocked ? t("learning.units.lockedHint") : undefined}
        onClick={() => onGo(page + 1)}
      >
        {t("learning.units.nextPage")}
        {blocked ? (
          <Lock className="h-4 w-4" aria-hidden />
        ) : (
          <ChevronRight className="h-4 w-4" aria-hidden />
        )}
      </Button>
    </nav>
  );
}

export function UnitLearningView({ unitId, quizId = null, page = 1 }: Props) {
  const { t } = useTranslation();
  const [data, setData] = useState<LearningResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);
  const pendingScroll = useRef<{ anchor: string; upOnly: boolean } | null>(
    null,
  );
  const navigate = useNavigate();

  const reload = () => {
    getLearning()
      .then(setData)
      .catch(() => setError(t("learning.loadError")));
  };

  useEffect(() => {
    setLoading(true);
    setError(null);
    getLearning()
      .then(setData)
      .catch(() => setError(t("learning.loadError")))
      .finally(() => setLoading(false));
  }, [t]);

  useEffect(() => {
    if (unitId == null) return;
    markUnitSeen(unitId)
      .then(() => notifyBadgesChanged())
      .catch(() => {});
  }, [unitId]);

  useEffect(() => {
    setSheetOpen(false);
  }, [unitId, quizId]);

  const groups = useMemo(
    () =>
      data
        ? buildGroups(data.sharedPresentations, data.homework, data.unitQuizzes)
        : [],
    [data],
  );
  const group = data ? findUnitGroup(groups, unitId) : undefined;
  // A real unit gets the contents panel; the "Otros" bucket keeps its plain layout.
  const unitGroup = unitId !== null ? group : undefined;
  const outline = useMemo(
    () => (unitGroup ? buildUnitOutline(unitGroup) : null),
    [unitGroup],
  );
  const pageCount =
    outline?.entries.filter((e) => e.kind === "page").length ?? 0;
  // Pages after an activity stay locked until it is submitted: the student can go up to
  // (and including) the first page whose activity is still pending.
  const firstPendingActivityPage = outline?.entries.find(
    (e) => e.kind === "page" && e.activity?.status === "PENDING",
  );
  const lastUnlockedPage =
    firstPendingActivityPage?.kind === "page"
      ? firstPendingActivityPage.page
      : pageCount;
  // The unit's end quizzes stay locked until every activity is submitted.
  const quizzesLocked = firstPendingActivityPage !== undefined;
  // Only an unlocked quiz that ends this unit opens here; anything else shows the PDF.
  const openQuiz = quizzesLocked
    ? null
    : (unitGroup?.quizzes.find((q) => q.id === quizId) ?? null);
  const currentPage = Math.min(
    Math.max(page, 1),
    Math.max(lastUnlockedPage, 1),
  );
  // Only the shown page and its activity can be in view.
  const activeAnchorId = useActiveUnitAnchor(
    openQuiz || !outline
      ? []
      : outline.entries.flatMap((e) =>
          e.kind !== "quiz" && e.page === currentPage ? [e.anchorId] : [],
        ),
  );

  /** Shows `target` (address only, no scroll). */
  const showPage = (target: number) => {
    if (unitId === null || target === currentPage || target > lastUnlockedPage)
      return;
    void navigate({
      to: "/aprendizaje/unidad/$unitId",
      params: { unitId },
      search: { page: target },
      replace: true,
      resetScroll: false,
    });
  };

  /**
   * Previous/next (arrows, ← →): never scrolls down. Only when the new page starts above
   * the screen (e.g. a page turned with ← → while scrolled down) does it scroll back up to its top.
   */
  const turnPage = (target: number) => {
    if (target < 1 || target > lastUnlockedPage) return;
    pendingScroll.current = { anchor: pageAnchorId(target), upOnly: true };
    showPage(target);
  };

  // Scroll after the new page has been revealed.
  useEffect(() => {
    const pending = pendingScroll.current;
    if (!pending) return;
    pendingScroll.current = null;
    if (pending.upOnly) {
      const el = document.getElementById(pending.anchor);
      const margin = el ? parseFloat(getComputedStyle(el).scrollMarginTop) : 0;
      if (!el || el.getBoundingClientRect().top >= margin) return;
    }
    scrollToUnitAnchor(pending.anchor);
  }, [currentPage]);

  // ← / → turn pages, unless the student is typing or a quiz/sheet is open.
  useEffect(() => {
    if (openQuiz || sheetOpen || pageCount < 2) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.altKey || e.ctrlKey || e.metaKey || e.shiftKey) return;
      const el = e.target as HTMLElement | null;
      if (
        el &&
        (el.isContentEditable ||
          ["INPUT", "TEXTAREA", "SELECT"].includes(el.tagName))
      ) {
        return;
      }
      if (e.key === "ArrowLeft") turnPage(currentPage - 1);
      else if (e.key === "ArrowRight") turnPage(currentPage + 1);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  const pager =
    pageCount > 1 ? (
      <UnitPager
        page={currentPage}
        pageCount={pageCount}
        lastUnlockedPage={lastUnlockedPage}
        onGo={turnPage}
      />
    ) : null;
  const title =
    unitId === null
      ? t("learning.units.other")
      : (group?.label ?? t("learning.units.notFound"));

  if (!loading && !error && data && unitGroup) {
    const panelProps = {
      groups,
      current: unitGroup,
      activeAnchorId,
      openQuizId: openQuiz?.id ?? null,
      lastUnlockedPage,
      quizzesLocked,
    };
    return (
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
        <Link
          to="/aprendizaje"
          className="inline-flex items-center gap-1 text-sm md:hidden text-muted-foreground hover:text-foreground"
        >
          {t("learning.units.back")}
        </Link>

        <div className="sticky top-[4.25rem] z-30 -mx-4 mt-4 bg-background/95 px-4 py-2 backdrop-blur sm:-mx-6 sm:px-6 md:hidden">
          <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setSheetOpen(true)}
            >
              <ListTree className="h-4 w-4" aria-hidden />
              {t("learning.units.openContents")}
            </Button>
            <SheetContent
              side="left"
              className="w-[85vw] overflow-y-auto"
              data-unit-contents-scroll=""
            >
              <SheetHeader className="mb-4">
                <SheetTitle>{t("learning.units.contents")}</SheetTitle>
              </SheetHeader>
              <UnitContentsPanel
                {...panelProps}
                onSelectAnchor={(_anchor, page) => {
                  showPage(page);
                  setSheetOpen(false);
                }}
                onNavigate={() => setSheetOpen(false)}
              />
            </SheetContent>
          </Sheet>
        </div>

        <div className="mt-6 md:mt-0 md:flex md:items-start md:gap-8">
          <aside
            className="sticky top-24 hidden max-h-[calc(100vh-7rem)] w-72 shrink-0 overflow-y-auto md:block"
            data-unit-contents-scroll=""
          >
            <Link
              to="/aprendizaje"
              className="mb-4 inline-flex items-center gap-1 px-2 text-sm text-muted-foreground hover:text-foreground"
            >
              {t("learning.units.back")}
            </Link>
            <UnitContentsPanel
              {...panelProps}
              onSelectAnchor={(_anchor, page) => showPage(page)}
            />
          </aside>

          <div key={unitId} className="min-w-0 flex-1">
            {openQuiz && (
              <div className="space-y-6">
                <Link
                  to="/aprendizaje/unidad/$unitId"
                  params={{ unitId: unitGroup.unitId as string }}
                  search={{ page: currentPage }}
                  className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                >
                  {t("learning.units.backToUnit")}
                </Link>
                <QuizTakeView
                  key={openQuiz.id}
                  quizId={openQuiz.id}
                  onChanged={reload}
                />
              </div>
            )}
            {/* Kept mounted (hidden) while a quiz is open so the PDF is not fetched again. */}
            <div hidden={openQuiz !== null} className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-x-6 gap-y-3">
                <h1 className="font-display text-2xl font-bold text-foreground">
                  {title}
                </h1>
                {unitGroup.presentations.length > 0 && pager}
              </div>
              {unitGroup.presentations.length > 0 &&
                currentPage >= lastUnlockedPage &&
                currentPage < pageCount && (
                  <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <Lock className="h-4 w-4 shrink-0" aria-hidden />
                    {t("learning.units.lockedHint")}
                  </p>
                )}
              {/* One page at a time, its activity right below it. */}
              {unitGroup.presentations.length > 0 ? (
                unitGroup.presentations.map((p) => (
                  <UnitPdf
                    key={p.id}
                    presentation={p}
                    currentPage={currentPage}
                    onChanged={reload}
                  />
                ))
              ) : (
                <p className="text-sm text-muted-foreground">
                  {t("learning.units.unitEmpty")}
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-8 px-6 py-10">
      <Link
        to="/aprendizaje"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.units.back")}
      </Link>

      {loading && <UnitSkeleton />}

      {error && <p className="text-destructive">{error}</p>}

      {!loading && !error && data && (
        <>
          <h1 className="font-display text-2xl font-bold text-foreground">
            {title}
          </h1>

          {!group ? (
            <p className="text-sm text-muted-foreground">
              {t("learning.units.notFound")}
            </p>
          ) : (
            <UnitDetailContent
              presentations={group.presentations}
              homework={group.homework}
              onHomeworkChanged={reload}
            />
          )}
        </>
      )}
    </div>
  );
}
