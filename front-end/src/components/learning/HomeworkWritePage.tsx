import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  getLearning,
  submitHomework,
  type HomeworkItem,
  type ApiError,
} from "@/lib/learning";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

const MAX_LENGTH = 2000;

const draftKey = (homeworkId: string) => `kuky:homework-draft:${homeworkId}`;

interface Props {
  homeworkId: string;
}

export function HomeworkWritePage({ homeworkId }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [item, setItem] = useState<HomeworkItem | null>(null);
  const [text, setText] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Guards autosave so it never fires before the draft/response is loaded.
  const loadedRef = useRef(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Grow the textarea to fit its content so the page scrolls, not the box.
  const autoGrow = () => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${el.scrollHeight}px`;
  };

  // Re-fit on content change (incl. initial load) and on width changes.
  useEffect(() => {
    autoGrow();
  }, [text]);

  useEffect(() => {
    window.addEventListener("resize", autoGrow);
    return () => window.removeEventListener("resize", autoGrow);
  }, []);

  useEffect(() => {
    getLearning()
      .then((data) => {
        const found = data.homework.find(
          (h) => h.id === homeworkId && h.format === "MANUAL",
        );
        if (!found) {
          setLoadError(t("learning.writePage.notFound"));
          return;
        }
        setItem(found);

        // Prefer a locally saved draft (it is the most recent edit) over the
        // last response persisted on the server — unless the homework is locked.
        const readOnly = found.status === "REVIEWED";
        let draft: string | null = null;
        if (!readOnly) {
          try {
            draft = localStorage.getItem(draftKey(homeworkId));
          } catch {
            draft = null;
          }
        }
        setText(draft ?? found.response ?? "");
        loadedRef.current = true;
      })
      .catch(() => setLoadError(t("learning.writePage.loadError")))
      .finally(() => setLoading(false));
  }, [homeworkId]);

  const readOnly = item?.status === "REVIEWED";

  const handleChange = (value: string) => {
    setText(value);
    if (!loadedRef.current || readOnly) return;
    // Persist synchronously so a reload or crash never loses progress.
    try {
      localStorage.setItem(draftKey(homeworkId), value);
    } catch {
      // Storage unavailable (private mode / quota) — submit still works.
    }
  };

  const handleSubmit = async () => {
    if (!item) return;
    setSubmitting(true);
    setError(null);
    try {
      await submitHomework(item.id, text.trim() || undefined);
      try {
        localStorage.removeItem(draftKey(homeworkId));
      } catch {
        // ignore
      }
      navigate({ to: "/aprendizaje" });
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
    <div className="mx-auto max-w-2xl px-6 py-12">
      <Link
        to="/aprendizaje"
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.writePage.back")}
      </Link>

      {loading && (
        <p className="mt-6 animate-pulse text-sm text-muted-foreground">
          {t("learning.writePage.loading")}
        </p>
      )}

      {loadError && !item && (
        <p className="mt-6 text-sm text-destructive">{loadError}</p>
      )}

      {item && (
        <>
          <h1 className="font-display text-3xl font-semibold text-primary">
            {item.title}
          </h1>
          <p className="mt-2 whitespace-pre-wrap text-muted-foreground">
            {item.instructions}
          </p>

          <div className="mt-8 space-y-3">
            <div className="flex items-baseline justify-between gap-3">
              <label
                htmlFor="homework-response"
                className="text-sm font-medium text-foreground"
              >
                {t("learning.writePage.yourAnswer")}
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
              placeholder={t("learning.writePage.placeholder")}
              rows={14}
              maxLength={MAX_LENGTH}
              disabled={readOnly || submitting}
              className="min-h-[18rem] resize-none overflow-hidden"
            />

            {!readOnly && (
              <p className="text-xs text-muted-foreground">
                {t("learning.writePage.autosaveHint")}
              </p>
            )}

            {error && <p className="text-sm text-destructive">{error}</p>}

            {!readOnly && (
              <div className="flex items-center gap-3 pt-1">
                <Button onClick={handleSubmit} disabled={submitting}>
                  {submitting
                    ? t("learning.writePage.submitting")
                    : t("learning.writePage.submit")}
                </Button>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
