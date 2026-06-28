import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  setUnitAssignees,
  studentDisplayName,
  type UnitSummary,
  type UnitDetail,
  type Student,
} from "@/lib/admin";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  unit: UnitSummary;
  allStudents: Student[];
  onAssigned: (detail: UnitDetail) => void;
}

export function UnitAssignDialog({
  open,
  onOpenChange,
  unit,
  allStudents,
  onAssigned,
}: Props) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<Set<string>>(
    new Set(unit.assignedStudentIds),
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggle = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const detail = await setUnitAssignees(unit.id, [...selected]);
      onAssigned(detail);
      onOpenChange(false);
    } catch {
      setError(t("admin.units.assignError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>
            {t("admin.units.assign")} — {unit.level} · {unit.subject}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-1 max-h-60 overflow-y-auto">
          {allStudents.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              {t("admin.units.noStudents")}
            </p>
          ) : (
            allStudents.map((s) => {
              const checked = selected.has(s.id);
              return (
                <label
                  key={s.id}
                  className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 hover:bg-muted text-sm"
                >
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => toggle(s.id)}
                    className="h-4 w-4 rounded"
                  />
                  <span>{studentDisplayName(s)}</span>
                  <span className="text-xs text-muted-foreground">
                    {s.email}
                  </span>
                </label>
              );
            })
          )}
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <DialogFooter>
          <Button
            variant="ghost"
            onClick={() => onOpenChange(false)}
            disabled={saving}
          >
            {t("common.cancel")}
          </Button>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? t("common.saving") : t("common.save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
