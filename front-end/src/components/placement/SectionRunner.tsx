import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  startSection,
  submitSection,
  type AnswerPayload,
  type ApiError,
  type SectionDto,
  type SectionResultResponse,
} from "@/lib/placement";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { PlacementQuestion, type AnswerState } from "./PlacementQuestion";

interface Props {
  attemptId: string;
  section: SectionDto;
  onSubmitted: (result: SectionResultResponse) => void;
}

/** Runs one timed section: countdown to the server-issued deadline, warn-then-allow
 * manual submit, and a silent auto-submit when the deadline elapses (FR-006/FR-007). */
export function SectionRunner({ attemptId, section, onSubmitted }: Props) {
  const { t } = useTranslation();
  const [deadlineAt, setDeadlineAt] = useState<string | null>(
    section.deadlineAt,
  );
  const [starting, setStarting] = useState(section.status === "NOT_STARTED");
  const [answers, setAnswers] = useState<Record<string, AnswerState>>(() =>
    Object.fromEntries(
      section.questions.map((q) => [
        q.id,
        { selectedOptionIds: [], answerText: "" },
      ]),
    ),
  );
  const [remainingMs, setRemainingMs] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [unansweredCount, setUnansweredCount] = useState(0);
  const submittedRef = useRef(false);
  // Mirrors `answers` so the timer's auto-submit (set up once per deadline, not
  // per keystroke) always reads the latest answers instead of a stale closure.
  const answersRef = useRef(answers);
  useEffect(() => {
    answersRef.current = answers;
  }, [answers]);

  useEffect(() => {
    if (section.status !== "NOT_STARTED") return;
    let cancelled = false;
    startSection(attemptId, section.skill)
      .then((res) => {
        if (!cancelled) setDeadlineAt(res.deadlineAt);
      })
      .catch((e) => {
        if (!cancelled)
          setError(
            (e as ApiError).message ?? t("placement.section.startError"),
          );
      })
      .finally(() => {
        if (!cancelled) setStarting(false);
      });
    return () => {
      cancelled = true;
    };
  }, [attemptId, section.skill, section.status]);

  const doSubmit = async (answerOverride?: AnswerPayload[]) => {
    if (submittedRef.current) return;
    submittedRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      const currentAnswers = answersRef.current;
      const payload: AnswerPayload[] =
        answerOverride ??
        section.questions.map((q) => ({
          questionId: q.id,
          selectedOptionIds: currentAnswers[q.id]?.selectedOptionIds ?? [],
          answerText:
            q.kind === "FILL_BLANK"
              ? (currentAnswers[q.id]?.answerText ?? "")
              : null,
        }));
      const result = await submitSection(attemptId, section.skill, payload);
      onSubmitted(result);
    } catch (e) {
      submittedRef.current = false;
      setError((e as ApiError).message ?? t("placement.section.submitError"));
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

  const totalLimitMs = section.timeLimitSeconds * 1000;
  const progressValue =
    totalLimitMs > 0 ? (remainingMs / totalLimitMs) * 100 : 0;
  const minutes = Math.floor(remainingMs / 60000);
  const seconds = Math.floor((remainingMs % 60000) / 1000);

  const handleManualSubmit = () => {
    const unanswered = section.questions.filter((q) => {
      const a = answers[q.id];
      if (!a) return true;
      return q.kind === "FILL_BLANK"
        ? !a.answerText.trim()
        : a.selectedOptionIds.length === 0;
    });
    if (unanswered.length > 0) {
      setUnansweredCount(unanswered.length);
      setConfirmOpen(true);
      return;
    }
    doSubmit();
  };

  const sectionLabel = useMemo(
    () => t(`placement.intro.sections.${section.skill}` as never),
    [section.skill, t],
  );

  if (starting) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("placement.section.loading")}
      </p>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <div className="flex items-center justify-between text-sm">
          <span className="font-medium">{sectionLabel}</span>
          <span className="text-muted-foreground">
            {t("placement.section.timeRemaining")}: {minutes}:
            {seconds.toString().padStart(2, "0")}
          </span>
        </div>
        <Progress value={progressValue} className="mt-2" />
      </div>

      <div className="space-y-6">
        {section.questions.map((q, i) => (
          <PlacementQuestion
            key={q.id}
            question={q}
            index={i}
            answer={answers[q.id] ?? { selectedOptionIds: [], answerText: "" }}
            onChange={(next) =>
              setAnswers((prev) => ({ ...prev, [q.id]: next }))
            }
          />
        ))}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button onClick={handleManualSubmit} disabled={submitting}>
        {submitting
          ? t("placement.section.submitting")
          : t("placement.section.submit")}
      </Button>

      <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t("placement.section.warnTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t("placement.section.warnBody", { count: unansweredCount })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>
              {t("placement.section.warnCancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                setConfirmOpen(false);
                doSubmit();
              }}
            >
              {t("placement.section.warnConfirm")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
