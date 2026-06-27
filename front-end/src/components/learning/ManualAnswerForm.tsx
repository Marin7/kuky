import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { submitHomework, type ApiError } from "@/lib/learning";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

const MAX_LENGTH = 2000;

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
  initialResponse: string | null;
  readOnly: boolean;
  labels: Labels;
  onSubmitted: () => void;
}

/**
 * Free-text homework answer with local autosave. The draft is persisted to
 * localStorage on every keystroke so a reload or crash never loses progress;
 * it is cleared once the homework is submitted. Shared by the writing and
 * reading pages.
 */
export function ManualAnswerForm({
  homeworkId,
  initialResponse,
  readOnly,
  labels,
  onSubmitted,
}: Props) {
  const { t } = useTranslation();
  const [text, setText] = useState(() => {
    // Prefer a locally saved draft (the most recent edit) over the last
    // server-saved response — unless the homework is locked.
    if (!readOnly && typeof window !== "undefined") {
      try {
        const draft = localStorage.getItem(draftKey(homeworkId));
        if (draft !== null) return draft;
      } catch {
        // ignore unavailable storage
      }
    }
    return initialResponse ?? "";
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Grow the textarea to fit its content so the page scrolls, not the box.
  const autoGrow = () => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${el.scrollHeight}px`;
  };

  useEffect(() => {
    autoGrow();
  }, [text]);

  useEffect(() => {
    window.addEventListener("resize", autoGrow);
    return () => window.removeEventListener("resize", autoGrow);
  }, []);

  const handleChange = (value: string) => {
    setText(value);
    if (readOnly) return;
    // Persist synchronously so a reload or crash never loses progress.
    try {
      localStorage.setItem(draftKey(homeworkId), value);
    } catch {
      // Storage unavailable (private mode / quota) — submit still works.
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await submitHomework(homeworkId, text.trim() || undefined);
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
      <div className="flex items-baseline justify-between gap-3">
        <label
          htmlFor="homework-response"
          className="text-sm font-medium text-foreground"
        >
          {labels.yourAnswer}
        </label>
        <span className="text-xs text-muted-foreground tabular-nums">
          {text.length} / {MAX_LENGTH}
        </span>
      </div>

      <Textarea
        ref={textareaRef}
        id="homework-response"
        value={text}
        onChange={(e) => handleChange(e.target.value)}
        placeholder={labels.placeholder}
        rows={14}
        maxLength={MAX_LENGTH}
        disabled={readOnly || submitting}
        className="min-h-[18rem] resize-none overflow-hidden"
      />

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
