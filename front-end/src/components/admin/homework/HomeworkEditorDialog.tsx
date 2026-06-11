import { useState } from "react";
import {
  createHomework,
  updateHomework,
  setAssignees,
  type HomeworkAdminItem,
  type ApiError,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { StudentMultiSelect } from "./StudentMultiSelect";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  existing: HomeworkAdminItem | null; // null ⇒ create
  onSaved: () => void;
}

export function HomeworkEditorDialog({
  open,
  onOpenChange,
  existing,
  onSaved,
}: Props) {
  const [title, setTitle] = useState(existing?.title ?? "");
  const [instructions, setInstructions] = useState(existing?.instructions ?? "");
  const [dueOn, setDueOn] = useState(existing?.dueOn ?? "");
  const [assigneeIds, setAssigneeIds] = useState<string[]>(
    existing?.assignees.map((a) => a.userId) ?? [],
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const save = async () => {
    if (!title.trim() || !instructions.trim()) {
      setError("El título y las instrucciones son obligatorios.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const due = dueOn ? dueOn : null;
      if (existing) {
        await updateHomework(existing.id, title, instructions, due);
        await setAssignees(existing.id, assigneeIds);
      } else {
        await createHomework(title, instructions, due, assigneeIds);
      }
      onSaved();
      onOpenChange(false);
    } catch (e) {
      setError((e as ApiError).message ?? "No se pudo guardar la tarea.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{existing ? "Editar tarea" : "Nueva tarea"}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-1">
            <Label htmlFor="hw-title">Título</Label>
            <Input
              id="hw-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="hw-instructions">Instrucciones</Label>
            <Textarea
              id="hw-instructions"
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              rows={4}
              maxLength={5000}
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="hw-due">Fecha límite (opcional)</Label>
            <Input
              id="hw-due"
              type="date"
              value={dueOn}
              onChange={(e) => setDueOn(e.target.value)}
              className="w-44"
            />
          </div>
          <div className="space-y-1">
            <Label>Asignar a</Label>
            <StudentMultiSelect
              selected={assigneeIds}
              onChange={setAssigneeIds}
            />
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={save} disabled={saving}>
            {saving ? "Guardando…" : "Guardar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
