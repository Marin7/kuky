import { useState } from "react";
import { useTranslation } from "react-i18next";
import { submitHomework, type ApiError } from "@/lib/learning";
import { Button } from "@/components/ui/button";
import { RichTextEditor } from "@/components/learning/richtext/RichTextEditor";
import { RichTextViewer } from "@/components/learning/richtext/RichTextViewer";
import {
  plainText,
  type FormattedText,
} from "@/components/learning/richtext/types";

const draftKey = (homeworkId: string) => `kuky:homework-draft:${homeworkId}`;

interface Labels {
  yourAnswer: string;
  placeholder: string;
  submit: string;
  submitting: string;
  autosaveHint: string;
}

interface Props {
  homeworkId: string;
  initialResponse: FormattedText | null;
  readOnly: boolean;
  labels: Labels;
  onSubmitted: () => void;
}

function loadDraft(homeworkId: string): FormattedText | null {
  if (typeof window === "undefined") return null;
  try {
    const draft = localStorage.getItem(draftKey(homeworkId));
    if (draft === null) return null;
    return JSON.parse(draft) as FormattedText;
  } catch {
    return null;
  }
}

/**
 * Formatted (color/highlight/strike) homework answer with local autosave. The
 * draft is persisted to localStorage on every edit so a reload or crash never
 * loses progress; it is cleared once the homework is submitted. Shared by the
 * writing and reading pages.
 */
export function ManualAnswerForm({
  homeworkId,
  initialResponse,
  readOnly,
  labels,
  onSubmitted,
}: Props) {
  const { t } = useTranslation();
  const [answer, setAnswer] = useState<FormattedText>(() => {
    // Prefer a locally saved draft (the most recent edit) over the last
    // server-saved response — unless the homework is locked.
    if (!readOnly) {
      const draft = loadDraft(homeworkId);
      if (draft !== null) return draft;
    }
    return initialResponse ?? [];
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (value: FormattedText) => {
    setAnswer(value);
    if (readOnly) return;
    try {
      localStorage.setItem(draftKey(homeworkId), JSON.stringify(value));
    } catch {
      // Storage unavailable (private mode / quota) — submit still works.
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const hasContent = plainText(answer).trim().length > 0;
      await submitHomework(homeworkId, hasContent ? answer : undefined);
      try {
        localStorage.removeItem(draftKey(homeworkId));
      } catch {
        // ignore
      }
      onSubmitted();
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
    <div className="mt-8 space-y-3">
      <label className="text-sm font-medium text-foreground">
        {labels.yourAnswer}
      </label>

      {readOnly ? (
        <div className="rounded-md border bg-muted/20 p-3">
          <RichTextViewer segments={answer} />
        </div>
      ) : (
        <RichTextEditor
          value={answer}
          onChange={handleChange}
          placeholder={labels.placeholder}
          disabled={submitting}
        />
      )}

      {!readOnly && (
        <p className="text-xs text-muted-foreground">{labels.autosaveHint}</p>
      )}

      {error && <p className="text-sm text-destructive">{error}</p>}

      {!readOnly && (
        <div className="flex items-center gap-3 pt-1">
          <Button onClick={handleSubmit} disabled={submitting}>
            {submitting ? labels.submitting : labels.submit}
          </Button>
        </div>
      )}
    </div>
  );
}
