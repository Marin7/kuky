import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  setAssignees,
  studentDisplayName,
  localIsoDate,
  type HomeworkAdminItem,
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
import { DueOnDateInput } from "./DueOnDateInput";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  homework: HomeworkAdminItem;
  allStudents: Student[];
  onAssigned: (item: HomeworkAdminItem) => void;
}

export function HomeworkAssignDialog({
  open,
  onOpenChange,
  homework,
  allStudents,
  onAssigned,
}: Props) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<Set<string>>(
    () => new Set(homework.assignees.map((a) => a.userId)),
  );
  const [dueOns, setDueOns] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      homework.assignees
        .filter((a) => a.dueOn)
        .map((a) => [a.userId, a.dueOn as string]),
    ),
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
    const today = localIsoDate();
    const ids = [...selected];
    const original = Object.fromEntries(
      homework.assignees.map((a) => [a.userId, a.dueOn ?? ""]),
    );
    const past = ids.some((id) => {
      const due = dueOns[id];
      return !!due && due < today && due !== original[id];
    });
    if (past) {
      setError(t("admin.homework.dueOnInPast"));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const updated = await setAssignees(
        homework.id,
        ids,
        null,
        ids
          .filter((userId) => {
            const due = dueOns[userId] || "";
            return !(due && due < today);
          })
          .map((userId) => ({
            userId,
            dueOn: dueOns[userId] || null,
          })),
      );
      onAssigned(updated);
      onOpenChange(false);
    } catch {
      setError(t("admin.homework.assignError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[85vh] max-w-2xl flex-col gap-4 sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {t("admin.homework.assign")} — {homework.title}
          </DialogTitle>
        </DialogHeader>

        <div className="min-h-0 flex-1 space-y-1 overflow-y-auto pr-1">
          {allStudents.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              {t("admin.homework.noStudents")}
            </p>
          ) : (
            <>
              <div className="sticky top-0 z-10 grid grid-cols-[auto_minmax(0,1fr)_9.5rem] items-center gap-3 border-b bg-background px-2 py-1.5 text-xs font-medium text-muted-foreground">
                <span className="w-4" />
                <span />
                <span>{t("admin.homework.dueOn")}</span>
              </div>
              {allStudents.map((s) => {
                const checked = selected.has(s.id);
                const due = checked ? (dueOns[s.id] ?? "") : "";
                return (
                  <div
                    key={s.id}
                    className="grid grid-cols-[auto_minmax(0,1fr)_9.5rem] items-start gap-3 rounded-md px-2 py-1.5 hover:bg-muted"
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggle(s.id)}
                      className="mt-2 h-4 w-4 rounded"
                      aria-label={studentDisplayName(s)}
                    />
                    <button
                      type="button"
                      className="min-w-0 cursor-pointer pt-1 text-left text-sm"
                      onClick={() => toggle(s.id)}
                    >
                      <span className="block truncate font-medium">
                        {studentDisplayName(s)}
                      </span>
                      <span className="block truncate text-xs text-muted-foreground">
                        {s.email}
                      </span>
                    </button>
                    <DueOnDateInput
                      value={due}
                      disabled={!checked}
                      onChange={(next) =>
                        setDueOns((prev) => ({
                          ...prev,
                          [s.id]: next,
                        }))
                      }
                    />
                  </div>
                );
              })}
            </>
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
