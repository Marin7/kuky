import {
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type RefObject,
} from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  getHomework,
  getStudents,
  deleteHomework,
  updateHomeworkLabels,
  type HomeworkAdminItem,
  type HomeworkType,
  type HomeworkLevel,
  type Student,
} from "@/lib/admin";
import {
  homeworkHasLabelGroup,
  homeworkLabels,
  labelGroupKey,
  labelsEqual,
  uniqueLabels,
} from "@/lib/homeworkLabels";
import { HomeworkAdminCard } from "@/components/admin/homework/HomeworkAdminCard";
import { HomeworkAssignDialog } from "@/components/admin/homework/HomeworkAssignDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
} from "@/components/ui/pagination";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ChevronLeft, ChevronRight, Search } from "lucide-react";
import { flushSync } from "react-dom";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { onBadgesInvalidate } from "@/lib/notifications";
import { cn } from "@/lib/utils";

const EXPAND_MS = 500;
const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 250;

type SortOption = "createdAtDesc" | "createdAtAsc" | "titleAsc" | "titleDesc";
const SORT_OPTIONS: SortOption[] = [
  "createdAtDesc",
  "createdAtAsc",
  "titleAsc",
  "titleDesc",
];

function normalizeForSearch(text: string): string {
  return text
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLocaleLowerCase("es")
    .trim();
}

function titleSortKey(title: string): string {
  return title.replace(/^[\s¿?]+/, "");
}

function compareTitles(a: HomeworkAdminItem, b: HomeworkAdminItem): number {
  return titleSortKey(a.title).localeCompare(titleSortKey(b.title), "es", {
    numeric: true,
    sensitivity: "base",
  });
}

// Relies on GET /admin/homework returning rows ORDER BY created_at DESC.
function sortHomework(
  list: HomeworkAdminItem[],
  sort: SortOption,
): HomeworkAdminItem[] {
  switch (sort) {
    case "createdAtDesc":
      return list;
    case "createdAtAsc":
      return [...list].reverse();
    case "titleAsc":
      return [...list].sort(compareTitles);
    case "titleDesc":
      return [...list].sort((a, b) => compareTitles(b, a));
  }
}

function pageWindow(current: number, total: number): (number | "gap")[] {
  const pages = new Set([0, total - 1, current - 1, current, current + 1]);
  const sorted = [...pages]
    .filter((p) => p >= 0 && p < total)
    .sort((a, b) => a - b);
  const result: (number | "gap")[] = [];
  for (const p of sorted) {
    const prev = result[result.length - 1];
    if (typeof prev === "number" && p - prev > 1) result.push("gap");
    result.push(p);
  }
  return result;
}

function useGridColumnCount(
  ref: RefObject<HTMLDivElement | null>,
  enabled: boolean,
): number {
  const [cols, setCols] = useState(1);

  useLayoutEffect(() => {
    const el = ref.current;
    if (!enabled || !el) return;
    const compute = () => {
      const raw = getComputedStyle(el).gridTemplateColumns;
      const count = raw.split(/\s+/).filter(Boolean).length;
      setCols(count || 1);
    };
    compute();
    const observer = new ResizeObserver(compute);
    observer.observe(el);
    return () => observer.disconnect();
  }, [ref, enabled]);

  return cols;
}

function expandedRowLayout(
  index: number,
  ids: string[],
  leadId: string | null,
  fullWidth: boolean,
  cols: number,
): { className: string; style?: { order: number } } {
  const isLead = leadId != null && ids[index] === leadId;
  if (!leadId || cols <= 1) {
    return { className: isLead && fullWidth ? "col-span-full" : "" };
  }
  const leadIndex = ids.indexOf(leadId);
  if (leadIndex < 0) return { className: "" };
  const rowStart = Math.floor(leadIndex / cols) * cols;
  const rowEnd = rowStart + cols;
  const inRow = index >= rowStart && index < rowEnd;
  const isFirstAfterRow = index === rowEnd;
  let firstSibling = -1;
  for (let i = rowStart; i < rowEnd && i < ids.length; i++) {
    if (ids[i] !== leadId) {
      firstSibling = i;
      break;
    }
  }
  const isFirstSibling = index === firstSibling;

  let order = 3;
  if (index < rowStart) order = 0;
  else if (isLead) order = 1;
  else if (inRow) order = 2;

  return {
    className: cn(
      isLead && fullWidth && "col-span-full",
      (isFirstSibling || isFirstAfterRow) && "col-start-1",
    ),
    style: { order },
  };
}

function captureCardRects(els: Map<string, HTMLElement>): Map<string, DOMRect> {
  const rects = new Map<string, DOMRect>();
  for (const [id, el] of els) {
    rects.set(id, el.getBoundingClientRect());
  }
  return rects;
}

function clearCardMotion(els: Map<string, HTMLElement>) {
  for (const el of els.values()) {
    el.style.transition = "";
    el.style.transform = "";
    el.style.transformOrigin = "";
    el.style.overflow = "";
    el.style.zIndex = "";
    const inner = el.firstElementChild as HTMLElement | null;
    if (inner) {
      inner.style.transition = "";
      inner.style.transform = "";
      inner.style.transformOrigin = "";
    }
  }
}

function playCardSlide(
  first: Map<string, DOMRect>,
  els: Map<string, HTMLElement>,
  durationMs: number,
  preferId?: string,
) {
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  const moving: HTMLElement[] = [];
  const inners: HTMLElement[] = [];
  for (const [id, firstRect] of first) {
    const el = els.get(id);
    if (!el) continue;
    const last = el.getBoundingClientRect();
    const dx = firstRect.left - last.left;
    const dy = firstRect.top - last.top;
    const sx =
      id === preferId && last.width > 1 ? firstRect.width / last.width : 1;
    if (Math.abs(dx) < 1 && Math.abs(dy) < 1 && Math.abs(sx - 1) < 0.02) {
      continue;
    }
    el.style.transition = "none";
    el.style.transformOrigin = "0 0";
    el.style.zIndex = id === preferId ? "2" : "1";
    if (Math.abs(sx - 1) >= 0.02) {
      el.style.overflow = "hidden";
      el.style.transform = `translate(${dx}px, ${dy}px) scaleX(${sx})`;
      const inner = el.firstElementChild as HTMLElement | null;
      if (inner) {
        inner.style.transition = "none";
        inner.style.transformOrigin = "0 0";
        inner.style.transform = `scaleX(${1 / sx})`;
        inners.push(inner);
      }
    } else {
      el.style.transform = `translate(${dx}px, ${dy}px)`;
    }
    moving.push(el);
  }
  if (moving.length === 0) return;
  void moving[0].offsetWidth;
  const ease = `transform ${durationMs}ms ease-out`;
  for (const el of moving) {
    el.style.transition = ease;
    el.style.transform = "";
  }
  for (const inner of inners) {
    inner.style.transition = ease;
    inner.style.transform = "";
  }
  window.setTimeout(() => clearCardMotion(els), durationMs + 50);
}

export function HomeworkAdminList() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [items, setItems] = useState<HomeworkAdminItem[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState<HomeworkType | "ALL">("ALL");
  const [filterLevel, setFilterLevel] = useState<HomeworkLevel | "ALL">("ALL");
  const [filterLabel, setFilterLabel] = useState<string>("ALL");
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, SEARCH_DEBOUNCE_MS);
  const [sort, setSort] = useState<SortOption>("createdAtDesc");
  const [page, setPage] = useState(0);
  const listTopRef = useRef<HTMLDivElement>(null);
  const [assignItem, setAssignItem] = useState<HomeworkAdminItem | null>(null);
  const [labelSavingId, setLabelSavingId] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [leadId, setLeadId] = useState<string | null>(null);
  const [fullWidth, setFullWidth] = useState(false);
  const [fromWidth, setFromWidth] = useState<number | null>(null);
  const collapseTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const expandTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const gridRef = useRef<HTMLDivElement>(null);
  const cardEls = useRef(new Map<string, HTMLDivElement>());

  const load = () => {
    setLoading(true);
    Promise.all([getHomework(), getStudents()])
      .then(([homework, studentList]) => {
        setItems(homework);
        setStudents(studentList);
      })
      .catch(() => {
        setItems([]);
        setStudents([]);
      })
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  useEffect(() => {
    return onBadgesInvalidate(() => {
      getHomework()
        .then(setItems)
        .catch(() => {});
    });
  }, []);

  const labelOptions = useMemo(() => uniqueLabels(items), [items]);

  useEffect(() => {
    if (filterLabel === "ALL") return;
    const keys = new Set(
      labelOptions.map((label) => labelGroupKey(label)).filter(Boolean),
    );
    if (!keys.has(filterLabel)) setFilterLabel("ALL");
  }, [filterLabel, labelOptions]);

  const persistCardLabels = async (item: HomeworkAdminItem, next: string[]) => {
    let previous = homeworkLabels(item);
    let skipped = false;
    setItems((prev) => {
      const current = prev.find((h) => h.id === item.id);
      previous = current ? homeworkLabels(current) : previous;
      if (labelsEqual(next, previous)) {
        skipped = true;
        return prev;
      }
      return prev.map((h) => (h.id === item.id ? { ...h, labels: next } : h));
    });
    if (skipped) return;
    setLabelSavingId(item.id);
    try {
      const updated = await updateHomeworkLabels(item.id, next);
      setItems((prev) => prev.map((h) => (h.id === updated.id ? updated : h)));
    } catch {
      setItems((prev) =>
        prev.map((h) => (h.id === item.id ? { ...h, labels: previous } : h)),
      );
    } finally {
      setLabelSavingId(null);
    }
  };

  const openCreate = () => navigate({ to: "/panel/tareas/nueva" });

  const openEdit = (item: HomeworkAdminItem) =>
    navigate({
      to: "/panel/tareas/$homeworkId",
      params: { homeworkId: item.id },
    });

  const remove = async (item: HomeworkAdminItem) => {
    if (!window.confirm(`¿Eliminar la tarea "${item.title}"?`)) return;
    await deleteHomework(item.id);
    load();
  };

  const query = normalizeForSearch(debouncedSearch);
  const filtered = sortHomework(
    items.filter((item) => {
      if (filterType !== "ALL" && item.homeworkType !== filterType)
        return false;
      if (filterLevel !== "ALL" && item.level !== filterLevel) return false;
      if (filterLabel !== "ALL") {
        if (!homeworkHasLabelGroup(item, filterLabel)) return false;
      }
      if (query && !normalizeForSearch(item.title).includes(query)) {
        return false;
      }
      return true;
    }),
    sort,
  );

  useEffect(() => {
    setPage(0);
  }, [query, filterType, filterLevel, filterLabel, sort]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages - 1);
  const pageStart = currentPage * PAGE_SIZE;
  const visible = filtered.slice(pageStart, pageStart + PAGE_SIZE);

  const goToPage = (next: number) => {
    setPage(next);
    listTopRef.current?.scrollIntoView({ block: "start", behavior: "smooth" });
  };

  const showGrid = !loading && visible.length > 0;
  const visibleIds = visible.map((item) => item.id);
  const visibleIdKey = visibleIds.join(",");
  const cols = useGridColumnCount(gridRef, showGrid);

  useEffect(
    () => () => {
      if (collapseTimer.current) clearTimeout(collapseTimer.current);
      if (expandTimer.current) clearTimeout(expandTimer.current);
    },
    [],
  );

  useEffect(() => {
    if (leadId && !visibleIds.includes(leadId)) {
      setExpandedId(null);
      setLeadId(null);
      setFullWidth(false);
      setFromWidth(null);
    }
  }, [visibleIdKey, leadId]);

  const handleExpand = (id: string, open: boolean) => {
    if (collapseTimer.current) clearTimeout(collapseTimer.current);
    if (expandTimer.current) clearTimeout(expandTimer.current);

    if (open) {
      const first = captureCardRects(cardEls.current);
      const width =
        cardEls.current.get(id)?.getBoundingClientRect().width ?? null;
      flushSync(() => {
        setExpandedId(null);
        setLeadId(id);
        setFullWidth(true);
        setFromWidth(width);
      });
      playCardSlide(first, cardEls.current, EXPAND_MS, id);
      expandTimer.current = setTimeout(() => {
        clearCardMotion(cardEls.current);
        setExpandedId(id);
      }, EXPAND_MS);
      return;
    }

    setExpandedId(null);
    collapseTimer.current = setTimeout(() => {
      clearCardMotion(cardEls.current);
      const first = captureCardRects(cardEls.current);
      flushSync(() => {
        setFullWidth(false);
        setLeadId(null);
        setFromWidth(null);
      });
      playCardSlide(first, cardEls.current, EXPAND_MS, id);
    }, EXPAND_MS);
  };

  return (
    <div ref={listTopRef} className="scroll-mt-4 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <div className="relative">
            <Search
              aria-hidden
              className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground"
            />
            <Input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t("admin.homework.searchPlaceholder")}
              aria-label={t("admin.homework.searchPlaceholder")}
              className="h-8 w-56 pl-7 text-xs"
            />
          </div>
          <Select value={sort} onValueChange={(v) => setSort(v as SortOption)}>
            <SelectTrigger
              className="h-8 w-44 text-xs"
              aria-label={t("admin.homework.sortLabel")}
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SORT_OPTIONS.map((option) => (
                <SelectItem key={option} value={option}>
                  {t(`admin.homework.sortOptions.${option}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={filterType}
            onValueChange={(v) => setFilterType(v as HomeworkType | "ALL")}
          >
            <SelectTrigger className="h-8 w-36 text-xs">
              <SelectValue placeholder={t("admin.homework.allTypes")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">
                {t("admin.homework.allTypes")}
              </SelectItem>
              <SelectItem value="AUDIO">
                {t("admin.homework.type.AUDIO")}
              </SelectItem>
              <SelectItem value="READ">
                {t("admin.homework.type.READ")}
              </SelectItem>
              <SelectItem value="WRITE">
                {t("admin.homework.type.WRITE")}
              </SelectItem>
              <SelectItem value="GRAMMAR">
                {t("admin.homework.type.GRAMMAR")}
              </SelectItem>
            </SelectContent>
          </Select>
          <Select
            value={filterLevel}
            onValueChange={(v) => setFilterLevel(v as HomeworkLevel | "ALL")}
          >
            <SelectTrigger className="h-8 w-40 text-xs">
              <SelectValue placeholder={t("admin.homework.allLevels")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">
                {t("admin.homework.allLevels")}
              </SelectItem>
              {(["A1", "A2", "B1", "B2", "C1", "C2"] as HomeworkLevel[]).map(
                (l) => (
                  <SelectItem key={l} value={l}>
                    {l}
                  </SelectItem>
                ),
              )}
            </SelectContent>
          </Select>
          <Select value={filterLabel} onValueChange={setFilterLabel}>
            <SelectTrigger className="h-8 w-44 text-xs">
              <SelectValue placeholder={t("admin.homework.allLabels")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">
                {t("admin.homework.allLabels")}
              </SelectItem>
              {labelOptions.map((label) => (
                <SelectItem
                  key={labelGroupKey(label) ?? label}
                  value={labelGroupKey(label) ?? label}
                >
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Button size="sm" onClick={openCreate}>
          {t("admin.homework.newTask")}
        </Button>
      </div>

      {loading ? (
        <p className="text-sm text-muted-foreground">
          {t("admin.homework.loading")}
        </p>
      ) : filtered.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {items.length === 0
            ? t("admin.homework.noTasks")
            : t("admin.homework.noTasksFiltered")}
        </p>
      ) : (
        <div
          ref={gridRef}
          className="grid grid-cols-1 items-start gap-3 sm:grid-cols-2 lg:grid-cols-4"
        >
          {visible.map((item, index) => {
            const layout = expandedRowLayout(
              index,
              visibleIds,
              leadId,
              fullWidth,
              cols,
            );
            return (
              <HomeworkAdminCard
                key={item.id}
                item={item}
                labelOptions={labelOptions}
                labelSaving={labelSavingId === item.id}
                expanded={expandedId === item.id}
                headerWidth={
                  item.id === leadId &&
                  fullWidth &&
                  expandedId !== item.id &&
                  fromWidth != null
                    ? fromWidth
                    : null
                }
                onExpandedChange={(open) => handleExpand(item.id, open)}
                layoutClassName={layout.className}
                layoutStyle={layout.style}
                layoutRef={(el) => {
                  if (el) cardEls.current.set(item.id, el);
                  else cardEls.current.delete(item.id);
                }}
                onPersistLabels={persistCardLabels}
                onAssign={setAssignItem}
                onEdit={openEdit}
                onDelete={remove}
                onUpdated={(updated) =>
                  setItems((prev) =>
                    prev.map((h) => (h.id === updated.id ? updated : h)),
                  )
                }
              />
            );
          })}
        </div>
      )}
      {showGrid && totalPages > 1 && (
        <div className="flex flex-col items-center gap-2">
          <Pagination aria-label={t("admin.homework.pagesLabel")}>
            <PaginationContent>
              <PaginationItem>
                <Button
                  variant="ghost"
                  size="sm"
                  className="gap-1 pl-2.5"
                  disabled={currentPage === 0}
                  onClick={() => goToPage(currentPage - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                  <span>{t("admin.homework.previousPage")}</span>
                </Button>
              </PaginationItem>
              {pageWindow(currentPage, totalPages).map((p, i) =>
                p === "gap" ? (
                  <PaginationItem key={`gap-${i}`}>
                    <PaginationEllipsis />
                  </PaginationItem>
                ) : (
                  <PaginationItem key={p}>
                    <Button
                      variant={p === currentPage ? "outline" : "ghost"}
                      size="icon"
                      className="h-8 w-8"
                      aria-current={p === currentPage ? "page" : undefined}
                      onClick={() => goToPage(p)}
                    >
                      {p + 1}
                    </Button>
                  </PaginationItem>
                ),
              )}
              <PaginationItem>
                <Button
                  variant="ghost"
                  size="sm"
                  className="gap-1 pr-2.5"
                  disabled={currentPage === totalPages - 1}
                  onClick={() => goToPage(currentPage + 1)}
                >
                  <span>{t("admin.homework.nextPage")}</span>
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </PaginationItem>
            </PaginationContent>
          </Pagination>
          <p className="text-xs text-muted-foreground">
            {t("admin.homework.pageSummary", {
              from: pageStart + 1,
              to: pageStart + visible.length,
              total: filtered.length,
            })}
          </p>
        </div>
      )}
      {assignItem && (
        <HomeworkAssignDialog
          open
          onOpenChange={(open) => {
            if (!open) setAssignItem(null);
          }}
          homework={assignItem}
          allStudents={students}
          onAssigned={(updated) => {
            setItems((prev) =>
              prev.map((h) => (h.id === updated.id ? updated : h)),
            );
          }}
        />
      )}
    </div>
  );
}
