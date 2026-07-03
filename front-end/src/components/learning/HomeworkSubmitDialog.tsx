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
import {
  submitHomework,
  type HomeworkItem,
  type ApiError,
} from "@/lib/learning";
import { RichTextEditor } from "@/components/learning/richtext/RichTextEditor";
import {
  plainText,
  type FormattedText,
} from "@/components/learning/richtext/types";

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
  const [answer, setAnswer] = useState<FormattedText>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [lastItemId, setLastItemId] = useState<string | null>(null);
  if (item && item.id !== lastItemId) {
    setLastItemId(item.id);
    setAnswer(item.response ?? []);
    setError(null);
  }

  const handleSubmit = async () => {
    if (!item) return;
    setSubmitting(true);
    setError(null);
    try {
      const hasContent = plainText(answer).trim().length > 0;
      const updated = await submitHomework(
        item.id,
        hasContent ? answer : undefined,
      );
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
          <label className="text-sm font-medium">
            {t("learning.submitDialog.yourAnswer")}
          </label>
          <RichTextEditor
            value={answer}
            onChange={setAnswer}
            placeholder={t("learning.submitDialog.placeholder")}
            disabled={submitting}
            rows={6}
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
