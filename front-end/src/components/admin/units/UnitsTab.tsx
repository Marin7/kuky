import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  listUnits,
  createUnit,
  getStudents,
  type UnitSummary,
  type HomeworkLevel,
  type Student,
} from "@/lib/admin";
import { UnitCard } from "./UnitCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const LEVELS: HomeworkLevel[] = ["A1", "A2", "B1", "B2", "C1", "C2"];

const LEVEL_CLASS: Record<HomeworkLevel, string> = {
  A1: "bg-green-100 text-green-700",
  A2: "bg-green-100 text-green-700",
  B1: "bg-teal-100 text-teal-700",
  B2: "bg-teal-100 text-teal-700",
  C1: "bg-indigo-100 text-indigo-700",
  C2: "bg-indigo-100 text-indigo-700",
};

export { LEVELS, LEVEL_CLASS };

export function UnitsTab() {
  const { t } = useTranslation();
  const [units, setUnits] = useState<UnitSummary[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);
  const [newLevel, setNewLevel] = useState<HomeworkLevel | "">("");
  const [newSubject, setNewSubject] = useState("");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    Promise.all([listUnits(), getStudents()])
      .then(([u, s]) => {
        setUnits(u);
        setStudents(s);
      })
      .catch(() => setError(t("admin.units.loadError")))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handleCreate = async () => {
    if (!newLevel || !newSubject.trim()) return;
    setCreating(true);
    setError(null);
    try {
      const detail = await createUnit(
        newLevel as HomeworkLevel,
        newSubject.trim(),
      );
      setNewLevel("");
      setNewSubject("");
      setUnits((prev) => [
        ...prev,
        {
          id: detail.id,
          level: detail.level,
          subject: detail.subject,
          position: detail.position,
          presentationCount: detail.presentations.length,
          homeworkCount: detail.homeworks.length,
          assignedStudentIds: detail.assignedStudents.map((s) => s.id),
        },
      ]);
    } catch {
      setError(t("admin.units.saveError"));
    } finally {
      setCreating(false);
    }
  };

  const handleDeleted = (id: string) =>
    setUnits((prev) => prev.filter((u) => u.id !== id));

  const handleUpdated = (updated: UnitSummary) =>
    setUnits((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));

  const handleReordered = (reordered: UnitSummary[]) => {
    const level = reordered[0]?.level;
    if (!level) return;
    setUnits((prev) => [
      ...prev.filter((u) => u.level !== level),
      ...reordered,
    ]);
  };

  // Group by level, then sort by position
  const grouped = LEVELS.reduce<Record<string, UnitSummary[]>>((acc, lvl) => {
    const items = units
      .filter((u) => u.level === lvl)
      .sort((a, b) => a.position - b.position);
    if (items.length > 0) acc[lvl] = items;
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      {/* Create form */}
      <div className="flex flex-wrap items-end gap-2">
        <Select
          value={newLevel}
          onValueChange={(v) => setNewLevel(v as HomeworkLevel)}
        >
          <SelectTrigger className="h-9 w-28 text-xs">
            <SelectValue placeholder={t("admin.units.levelPlaceholder")} />
          </SelectTrigger>
          <SelectContent>
            {LEVELS.map((l) => (
              <SelectItem key={l} value={l}>
                {l}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input
          className="h-9 flex-1 min-w-48 text-xs"
          placeholder={t("admin.units.subjectPlaceholder")}
          value={newSubject}
          onChange={(e) => setNewSubject(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleCreate()}
          maxLength={200}
        />
        <Button
          size="sm"
          onClick={handleCreate}
          disabled={creating || !newLevel || !newSubject.trim()}
        >
          {creating ? t("admin.units.creating") : t("admin.units.newUnit")}
        </Button>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      {loading ? (
        <p className="text-sm text-muted-foreground">
          {t("admin.units.loading")}
        </p>
      ) : units.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("admin.units.empty")}
        </p>
      ) : (
        Object.entries(grouped).map(([level, levelUnits]) => (
          <div key={level}>
            <div className="mb-2 flex items-center gap-2">
              <span
                className={[
                  "rounded-full px-2.5 py-0.5 text-xs font-semibold",
                  LEVEL_CLASS[level as HomeworkLevel],
                ].join(" ")}
              >
                {level}
              </span>
              <span className="text-xs text-muted-foreground">
                {levelUnits.length}{" "}
                {levelUnits.length === 1 ? "unidad" : "unidades"}
              </span>
            </div>
            <div className="space-y-3">
              {levelUnits.map((unit) => (
                <UnitCard
                  key={unit.id}
                  unit={unit}
                  allLevelUnits={levelUnits}
                  students={students}
                  onDeleted={() => handleDeleted(unit.id)}
                  onUpdated={handleUpdated}
                  onReordered={handleReordered}
                />
              ))}
            </div>
          </div>
        ))
      )}
    </div>
  );
}
