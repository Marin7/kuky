import { useState } from "react";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
  const [text, setText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
        setError(t("learning.submitDialog.validationError"));
      } else if (err.error === "SUBMISSION_NOT_ALLOWED") {
        setError(t("learning.submitDialog.submissionNotAllowedError"));
      } else {
        setError(t("learning.submitDialog.genericError"));
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
            {t("learning.submitDialog.yourAnswer")}
          </label>
          <Textarea
            id="homework-response"
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={t("learning.submitDialog.placeholder")}
            rows={6}
            maxLength={2000}
          />
          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={submitting}>
            {t("learning.submitDialog.cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={submitting}>
            {submitting
              ? t("learning.submitDialog.submitting")
              : t("learning.submitDialog.submit")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
