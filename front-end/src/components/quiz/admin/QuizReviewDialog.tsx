import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getQuizAttempt, reviewQuizAttempt } from "@/lib/admin";
import type { QuizTakeResponse } from "@/lib/quiz";
import { notifyBadgesChanged } from "@/lib/notifications";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { RichTextEditor } from "@/components/learning/richtext/RichTextEditor";
import { RichTextViewer } from "@/components/learning/richtext/RichTextViewer";
import { type FormattedText } from "@/components/learning/richtext/types";
import { QuestionResultBlock } from "@/components/learning/ExerciseResult";
import type { QuestionResult } from "@/lib/learning";

function toFormatted(
  text: string | null | undefined,
  formatted?: FormattedText | null,
): FormattedText {
  if (formatted && formatted.length > 0) return formatted;
  if (!text) return [];
  return [{ text }];
}

function toQuestionResult(
  questionId: string,
  result: QuizTakeResponse["results"][number] | undefined,
): QuestionResult {
  return {
    questionId,
    score: result?.score ?? 0,
    correct: result?.correct ?? false,
    correctOptionIds: result?.correctOptionIds ?? [],
    acceptedAnswers: result?.acceptedAnswers ?? [],
    unitResults: result?.unitResults,
    selectedOptionIds: result?.selectedOptionIds,
  };
}

export function QuizReviewDialog({
  quizId,
  attemptId,
  onClose,
  onSaved,
}: {
  quizId: string;
  attemptId: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { t } = useTranslation();
  const [detail, setDetail] = useState<QuizTakeResponse | null>(null);
  const [percents, setPercents] = useState<Record<string, string>>({});
  const [formats, setFormats] = useState<Record<string, FormattedText>>({});
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getQuizAttempt(quizId, attemptId).then((d) => {
      setDetail(d);
      setNote(d.feedback ?? "");
      const next: Record<string, string> = {};
      const nextFormats: Record<string, FormattedText> = {};
      const results = d.results ?? [];
      for (const r of results) {
        if (r.teacherPercent != null) next[r.questionId] = String(r.teacherPercent);
        nextFormats[r.questionId] = toFormatted(r.answerText, r.formatted);
      }
      setPercents(next);
      setFormats(nextFormats);
      notifyBadgesChanged();
    });
  }, [quizId, attemptId]);

  const freeText = detail?.questions.filter((q) => q.kind === "FREE_TEXT") ?? [];
  const locked = detail?.status === "GRADED";

  const save = async (finalize: boolean) => {
    if (locked) return;
    setSaving(true);
    setError(null);
    try {
      await reviewQuizAttempt(quizId, attemptId, {
        answers: freeText.map((q) => ({
          questionId: q.id,
          teacherScorePercent:
            percents[q.id] === "" || percents[q.id] == null
              ? null
              : Number(percents[q.id]),
          formatted:
            formats[q.id] && formats[q.id].length > 0 ? formats[q.id] : null,
        })),
        feedbackText: note,
        finalize,
      });
      onSaved();
      if (finalize) onClose();
      else {
        const d = await getQuizAttempt(quizId, attemptId);
        setDetail(d);
        const nextFormats: Record<string, FormattedText> = {};
        for (const r of d.results ?? []) {
          nextFormats[r.questionId] = toFormatted(r.answerText, r.formatted);
        }
        setFormats(nextFormats);
      }
    } catch (e) {
      setError((e as { message?: string }).message ?? t("quiz.admin.saveError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {locked ? t("quiz.admin.view") : t("quiz.admin.review")}
          </DialogTitle>
        </DialogHeader>
        {!detail ? (
          <p className="text-sm text-muted-foreground">{t("common.loading")}</p>
        ) : (
          <div className="space-y-4">
            {detail.questions.map((q, i) => {
              const result = (detail.results ?? []).find((r) => r.questionId === q.id);
              const isFreeText = q.kind === "FREE_TEXT";
              return (
                <div key={q.id} className="space-y-2 rounded-md border p-3">
                  {q.skill && q.skill !== detail.questions[i - 1]?.skill && (
                    <p className="text-xs font-medium uppercase tracking-wide text-primary">
                      {t(`quiz.skills.${q.skill}`)}
                    </p>
                  )}
                  {isFreeText ? (
                    <>
                      <p className="text-sm font-medium">{q.prompt}</p>
                      {locked ? (
                        <div className="text-sm">
                          <RichTextViewer
                            segments={
                              formats[q.id] ??
                              toFormatted(result?.answerText, result?.formatted)
                            }
                          />
                        </div>
                      ) : (
                        <RichTextEditor
                          value={
                            formats[q.id] ??
                            toFormatted(result?.answerText, result?.formatted)
                          }
                          onChange={(next) =>
                            setFormats((prev) => ({ ...prev, [q.id]: next }))
                          }
                          formatOnly
                          rows={4}
                          disabled={saving}
                        />
                      )}
                      {locked ? (
                        result?.teacherPercent != null && (
                          <p className="text-xs font-medium">
                            {t("quiz.admin.teacherPercent")}: {result.teacherPercent}%
                          </p>
                        )
                      ) : (
                        <>
                          <Label>{t("quiz.admin.teacherPercent")}</Label>
                          <Input
                            type="number"
                            min={0}
                            max={100}
                            value={percents[q.id] ?? ""}
                            onChange={(e) =>
                              setPercents((prev) => ({
                                ...prev,
                                [q.id]: e.target.value,
                              }))
                            }
                          />
                        </>
                      )}
                    </>
                  ) : (
                    <QuestionResultBlock
                      question={q}
                      result={toQuestionResult(q.id, result)}
                      number={i + 1}
                      showAllAnswers
                    />
                  )}
                </div>
              );
            })}
            <div className="space-y-1">
              <Label>{t("quiz.admin.note")}</Label>
              {locked ? (
                note.trim() ? (
                  <p className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap">
                    {note}
                  </p>
                ) : (
                  <p className="text-sm text-muted-foreground">—</p>
                )
              ) : (
                <Textarea
                  value={note}
                  maxLength={500}
                  onChange={(e) => setNote(e.target.value)}
                />
              )}
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            {locked ? (
              <Button variant="outline" onClick={onClose}>
                {t("common.close")}
              </Button>
            ) : (
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  disabled={saving}
                  onClick={() => save(false)}
                >
                  {t("quiz.admin.saveProgress")}
                </Button>
                <Button disabled={saving} onClick={() => save(true)}>
                  {t("quiz.admin.finalize")}
                </Button>
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
