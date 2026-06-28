import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  updateUnit,
  deleteUnit,
  reorderUnits,
  type UnitSummary,
  type HomeworkLevel,
  type Student,
  type ApiError,
} from "@/lib/admin";
import { StudentLink } from "@/components/admin/students/StudentLink";
import { UnitContentPicker } from "./UnitContentPicker";
import { UnitAssignDialog } from "./UnitAssignDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LEVELS, LEVEL_CLASS } from "./UnitsTab";

interface Props {
  unit: UnitSummary;
  allLevelUnits: UnitSummary[];
  students: Student[];
  onDeleted: () => void;
  onUpdated: (updated: UnitSummary) => void;
  onReordered: (reordered: UnitSummary[]) => void;
}

export function UnitCard({
  unit,
  allLevelUnits,
  students,
  onDeleted,
  onUpdated,
  onReordered,
}: Props) {
  const { t } = useTranslation();
  const [editing, setEditing] = useState(false);
  const [editLevel, setEditLevel] = useState<HomeworkLevel>(unit.level);
  const [editSubject, setEditSubject] = useState(unit.subject);
  const [saving, setSaving] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const detail = await updateUnit(unit.id, editLevel, editSubject.trim());
      setEditing(false);
      onUpdated({
        ...unit,
        level: detail.level,
        subject: detail.subject,
        position: detail.position,
      });
    } catch {
      setError(t("admin.units.saveError"));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(t("admin.units.deleteConfirm"))) return;
    try {
      await deleteUnit(unit.id);
      onDeleted();
    } catch (err) {
      setError((err as ApiError).message ?? t("admin.units.deleteError"));
    }
  };

  const handleMoveUp = async () => {
    const idx = allLevelUnits.findIndex((u) => u.id === unit.id);
    if (idx <= 0) return;
    const newOrder = [...allLevelUnits];
    [newOrder[idx - 1], newOrder[idx]] = [newOrder[idx], newOrder[idx - 1]];
    try {
      const updated = await reorderUnits(
        unit.level,
        newOrder.map((u) => u.id),
      );
      onReordered(
        updated.map((s, i) => ({ ...newOrder[i], position: s.position })),
      );
    } catch {
      setError(t("admin.units.reorderError"));
    }
  };

  const handleMoveDown = async () => {
    const idx = allLevelUnits.findIndex((u) => u.id === unit.id);
    if (idx >= allLevelUnits.length - 1) return;
    const newOrder = [...allLevelUnits];
    [newOrder[idx], newOrder[idx + 1]] = [newOrder[idx + 1], newOrder[idx]];
    try {
      const updated = await reorderUnits(
        unit.level,
        newOrder.map((u) => u.id),
      );
      onReordered(
        updated.map((s, i) => ({ ...newOrder[i], position: s.position })),
      );
    } catch {
      setError(t("admin.units.reorderError"));
    }
  };

  const idx = allLevelUnits.findIndex((u) => u.id === unit.id);
  const assignedStudents = students.filter((s) =>
    unit.assignedStudentIds.includes(s.id),
  );

  return (
    <Card>
      <CardContent className="pt-4 space-y-3">
        {/* Header row */}
        {editing ? (
          <div className="flex flex-wrap items-center gap-2">
            <Select
              value={editLevel}
              onValueChange={(v) => setEditLevel(v as HomeworkLevel)}
            >
              <SelectTrigger className="h-8 w-24 text-xs">
                <SelectValue />
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
              autoFocus
              value={editSubject}
              onChange={(e) => setEditSubject(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleSave();
                if (e.key === "Escape") setEditing(false);
              }}
              className="flex-1 h-8 text-xs"
              maxLength={200}
            />
            <Button
              size="sm"
              className="h-8 text-xs"
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? t("common.saving") : t("common.save")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 text-xs"
              onClick={() => setEditing(false)}
            >
              {t("common.cancel")}
            </Button>
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-2">
            {/* Reorder arrows */}
            <div className="flex flex-col gap-0.5 shrink-0">
              <button
                className="text-muted-foreground hover:text-foreground disabled:opacity-30 text-xs leading-none"
                onClick={handleMoveUp}
                disabled={idx <= 0}
                aria-label="Mover arriba"
              >
                ▲
              </button>
              <button
                className="text-muted-foreground hover:text-foreground disabled:opacity-30 text-xs leading-none"
                onClick={handleMoveDown}
                disabled={idx >= allLevelUnits.length - 1}
                aria-label="Mover abajo"
              >
                ▼
              </button>
            </div>

            <span
              className={[
                "shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold",
                LEVEL_CLASS[unit.level],
              ].join(" ")}
            >
              {unit.level}
            </span>
            <span className="font-medium flex-1 truncate">{unit.subject}</span>
            <span className="text-xs text-muted-foreground shrink-0">
              #{unit.position + 1}
            </span>

            <Button
              variant="ghost"
              size="sm"
              className="h-7 text-xs shrink-0"
              onClick={() => {
                setEditLevel(unit.level);
                setEditSubject(unit.subject);
                setEditing(true);
              }}
            >
              {t("admin.units.edit")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-7 text-xs text-destructive shrink-0"
              onClick={handleDelete}
            >
              {t("admin.units.delete")}
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="h-7 text-xs shrink-0"
              onClick={() => setAssignOpen(true)}
            >
              {t("admin.units.assign")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-7 text-xs shrink-0"
              onClick={() => setExpanded((e) => !e)}
            >
              {expanded ? "▲" : "▼"}{" "}
              {t(
                expanded
                  ? "admin.units.presentations"
                  : "admin.units.contents.presentations",
              )}
            </Button>
          </div>
        )}

        {/* Assigned students */}
        {assignedStudents.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {assignedStudents.map((s) => (
              <span
                key={s.id}
                className="rounded-full bg-muted px-2 py-0.5 text-xs"
              >
                <StudentLink student={s} />
              </span>
            ))}
          </div>
        )}

        {/* Expandable content */}
        {expanded && (
          <UnitContentPicker
            unitId={unit.id}
            onUpdated={(detail) =>
              onUpdated({
                ...unit,
                presentationCount: detail.presentations.length,
                homeworkCount: detail.homeworks.length,
              })
            }
          />
        )}

        {error && <p className="text-xs text-destructive">{error}</p>}
      </CardContent>

      {assignOpen && (
        <UnitAssignDialog
          open={assignOpen}
          onOpenChange={setAssignOpen}
          unit={unit}
          allStudents={students}
          onAssigned={(detail) => {
            onUpdated({
              ...unit,
              assignedStudentIds: detail.assignedStudents.map((s) => s.id),
            });
          }}
        />
      )}
    </Card>
  );
}
