import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  startWriting,
  submitWriting,
  type ApiError,
  type WritingSectionDto,
  type WritingSubmissionDto,
} from "@/lib/placement";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

interface Props {
  section: WritingSectionDto;
  onSubmitted: (submission: WritingSubmissionDto) => void;
}

/**
 * Timed Writing composer: starts a server-issued deadline on mount, shows a
 * countdown, and silently auto-submits on expiry — mirrors SectionRunner's
 * timer for the auto-graded sections (FR-006), including reading the latest
 * text from a ref inside the timer tick rather than the render closure, so a
 * response typed just before expiry isn't dropped by a stale auto-submit.
 */
export function WritingComposer({ section, onSubmitted }: Props) {
  const { t } = useTranslation();
  const [deadlineAt, setDeadlineAt] = useState<string | null>(
    section.deadlineAt,
  );
  const [starting, setStarting] = useState(section.status === "NOT_STARTED");
  const [body, setBody] = useState("");
  const bodyRef = useRef(body);
  useEffect(() => {
    bodyRef.current = body;
  }, [body]);

  const [remainingMs, setRemainingMs] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expired, setExpired] = useState(false);
  const submittedRef = useRef(false);

  useEffect(() => {
    if (section.status !== "NOT_STARTED") return;
    let cancelled = false;
    startWriting()
      .then((res) => {
        if (!cancelled) setDeadlineAt(res.deadlineAt);
      })
      .catch((e) => {
        if (!cancelled)
          setError(
            (e as ApiError).message ??
              t("placement.fullEvaluation.writingStartError"),
          );
      })
      .finally(() => {
        if (!cancelled) setStarting(false);
      });
    return () => {
      cancelled = true;
    };
  }, [section.status, t]);

  const doSubmit = async () => {
    if (submittedRef.current) return;
    const currentBody = bodyRef.current;
    if (!currentBody.trim()) {
      // Time ran out with nothing written — there's nothing valid to send
      // (the backend rejects a blank body regardless of timing).
      setExpired(true);
      return;
    }
    submittedRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      const saved = await submitWriting(currentBody);
      onSubmitted({
        id: saved.id,
        body: currentBody,
        submittedAt: saved.submittedAt,
      });
    } catch (e) {
      submittedRef.current = false;
      setError(
        (e as ApiError).message ??
          t("placement.fullEvaluation.writingSubmitError"),
      );
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    if (!deadlineAt) return;
    const tick = () => {
      const ms = new Date(deadlineAt).getTime() - Date.now();
      setRemainingMs(Math.max(0, ms));
      if (ms <= 0 && !submittedRef.current) {
        doSubmit();
      }
    };
    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deadlineAt]);

  const minutes = Math.floor(remainingMs / 60000);
  const seconds = Math.floor((remainingMs % 60000) / 1000);

  if (starting) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("placement.section.loading")}
      </p>
    );
  }

  if (expired) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("placement.fullEvaluation.writingExpired")}
      </p>
    );
  }

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        {t("placement.section.timeRemaining")}: {minutes}:
        {seconds.toString().padStart(2, "0")}
      </p>
      <Textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder={t("placement.fullEvaluation.writingPlaceholder")}
        rows={8}
      />
      {error && <p className="text-sm text-destructive">{error}</p>}
      <Button onClick={doSubmit} disabled={submitting || !body.trim()}>
        {submitting
          ? t("placement.fullEvaluation.writingSubmitting")
          : t("placement.fullEvaluation.writingSubmit")}
      </Button>
    </div>
  );
}
