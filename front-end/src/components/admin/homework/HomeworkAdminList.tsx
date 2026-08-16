import { useEffect, useMemo, useState } from "react";
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

  const filtered = items.filter((item) => {
    if (filterType !== "ALL" && item.homeworkType !== filterType) return false;
    if (filterLevel !== "ALL" && item.level !== filterLevel) return false;
    if (filterLabel !== "ALL") {
      if (!homeworkHasLabelGroup(item, filterLabel)) return false;
    }
    return true;
  });

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
        <div className="grid grid-cols-1 items-start gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((item) => (
            <HomeworkAdminCard
              key={item.id}
              item={item}
              labelOptions={labelOptions}
              labelSaving={labelSavingId === item.id}
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
          ))}
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
