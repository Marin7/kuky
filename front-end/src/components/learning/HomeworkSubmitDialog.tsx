import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  submitHomework,
  type HomeworkItem,
  type ApiError,
} from "@/lib/learning";

interface HomeworkSubmitDialogProps {
  item: HomeworkItem | null;
  onClose: () => void;
  onSubmitted: (updated: HomeworkItem) => void;
}

export function HomeworkSubmitDialog({
  item,
  onClose,
  onSubmitted,
}: HomeworkSubmitDialogProps) {
  const [text, setText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync the textarea with the selected item each time the dialog opens.
  const [lastItemId, setLastItemId] = useState<string | null>(null);
  if (item && item.id !== lastItemId) {
    setLastItemId(item.id);
    setText(item.response ?? "");
    setError(null);
  }

  const handleSubmit = async () => {
    if (!item) return;
    setSubmitting(true);
    setError(null);
    try {
      const updated = await submitHomework(item.id, text.trim() || undefined);
      onSubmitted(updated);
      onClose();
    } catch (e) {
      const err = e as ApiError;
      if (err.error === "VALIDATION_ERROR") {
        setError("La respuesta es demasiado larga (máximo 2000 caracteres).");
      } else if (err.error === "SUBMISSION_NOT_ALLOWED") {
        setError("Esta tarea ya ha sido revisada y no puede modificarse.");
      } else {
        setError("No se pudo enviar la tarea. Inténtalo de nuevo.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={item !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{item?.title}</DialogTitle>
          <DialogDescription>{item?.instructions}</DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          <label htmlFor="homework-response" className="text-sm font-medium">
            Tu respuesta (opcional)
          </label>
          <Textarea
            id="homework-response"
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="Escribe aquí tu respuesta… o deja el campo vacío y marca la tarea como hecha."
            rows={6}
            maxLength={2000}
          />
          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} disabled={submitting}>
            {submitting ? "Enviando…" : "Entregar tarea"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
