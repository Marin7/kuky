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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { flushSync } from "react-dom";
import { onBadgesInvalidate } from "@/lib/notifications";
import { cn } from "@/lib/utils";

const EXPAND_MS = 500;

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
      id === preferId && last.width > 1
        ? firstRect.width / last.width
        : 1;
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

  const filtered = items
    .filter((item) => {
      if (filterType !== "ALL" && item.homeworkType !== filterType) return false;
      if (filterLevel !== "ALL" && item.level !== filterLevel) return false;
      if (filterLabel !== "ALL") {
        if (!homeworkHasLabelGroup(item, filterLabel)) return false;
      }
      return true;
    })
    .sort((a, b) =>
      a.title.localeCompare(b.title, "es", { numeric: true, sensitivity: "base" }),
    );

  const showGrid = !loading && filtered.length > 0;
  const filteredIds = filtered.map((item) => item.id);
  const filteredIdKey = filteredIds.join(",");
  const cols = useGridColumnCount(gridRef, showGrid);

  useEffect(
    () => () => {
      if (collapseTimer.current) clearTimeout(collapseTimer.current);
      if (expandTimer.current) clearTimeout(expandTimer.current);
    },
    [],
  );

  useEffect(() => {
    if (leadId && !filteredIds.includes(leadId)) {
      setExpandedId(null);
      setLeadId(null);
      setFullWidth(false);
      setFromWidth(null);
    }
  }, [filteredIdKey, leadId]);

  const handleExpand = (id: string, open: boolean) => {
    if (collapseTimer.current) clearTimeout(collapseTimer.current);
    if (expandTimer.current) clearTimeout(expandTimer.current);

    if (open) {
      const first = captureCardRects(cardEls.current);
      const width = cardEls.current.get(id)?.getBoundingClientRect().width ?? null;
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
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
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
          {filtered.map((item, index) => {
            const layout = expandedRowLayout(
              index,
              filteredIds,
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
