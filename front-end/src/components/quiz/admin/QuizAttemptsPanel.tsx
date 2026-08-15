import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getQuizAttempt,
  listQuizAttempts,
  reviewQuizAttempt,
  type QuizAttemptListItem,
} from "@/lib/admin";
import type { QuizTakeResponse } from "@/lib/quiz";
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

export function QuizAttemptsPanel({ quizId }: { quizId: string }) {
  const { t } = useTranslation();
  const [attempts, setAttempts] = useState<QuizAttemptListItem[]>([]);
  const [openId, setOpenId] = useState<string | null>(null);

  const load = () => {
    listQuizAttempts(quizId).then(setAttempts).catch(() => setAttempts([]));
  };

  useEffect(load, [quizId]);

  return (
    <div className="space-y-3 border-t pt-6">
      <h2 className="text-lg font-semibold">{t("quiz.admin.attemptsTitle")}</h2>
      {attempts.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t("quiz.admin.noAttempts")}</p>
      ) : (
        <ul className="space-y-2">
          {attempts.map((a) => (
            <li key={a.id} className="flex items-center justify-between rounded-md border p-3 text-sm">
              <span>
                {a.studentName} · {t(`quiz.status.${a.status}` as never)}
                {a.scorePercent != null ? ` · ${a.scorePercent}%` : ""}
              </span>
              <Button size="sm" variant="outline" onClick={() => setOpenId(a.id)}>
                {t("quiz.admin.review")}
              </Button>
            </li>
          ))}
        </ul>
      )}
      {openId && (
        <QuizReviewDialog
          quizId={quizId}
          attemptId={openId}
          onClose={() => setOpenId(null)}
          onSaved={load}
        />
      )}
    </div>
  );
}

function QuizReviewDialog({
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
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getQuizAttempt(quizId, attemptId).then((d) => {
      setDetail(d);
      setNote(d.feedback ?? "");
      const next: Record<string, string> = {};
      const results = d.results ?? [];
      for (const r of results) {
        if (r.teacherPercent != null) next[r.questionId] = String(r.teacherPercent);
      }
      setPercents(next);
    });
  }, [quizId, attemptId]);

  const freeText = detail?.questions.filter((q) => q.kind === "FREE_TEXT") ?? [];

  const save = async (finalize: boolean) => {
    setSaving(true);
    setError(null);
    try {
      await reviewQuizAttempt(quizId, attemptId, {
        answers: freeText.map((q) => ({
          questionId: q.id,
          teacherScorePercent: percents[q.id] === "" || percents[q.id] == null
            ? null
            : Number(percents[q.id]),
        })),
        feedbackText: note,
        finalize,
      });
      onSaved();
      if (finalize) onClose();
      else {
        const d = await getQuizAttempt(quizId, attemptId);
        setDetail(d);
      }
    } catch (e) {
      setError((e as { message?: string }).message ?? t("quiz.admin.saveError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[85vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{t("quiz.admin.review")}</DialogTitle>
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
                  <p className="text-sm font-medium">{q.prompt}</p>
                  <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                    {result?.answerText ||
                      result?.selectedOptionIds?.join(", ") ||
                      "—"}
                  </p>
                  {!isFreeText && result && (
                    <p className="text-xs text-muted-foreground">
                      {Math.round(result.score * 100)}%
                    </p>
                  )}
                  {isFreeText && (
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
                </div>
              );
            })}
            <div className="space-y-1">
              <Label>{t("quiz.admin.note")}</Label>
              <Textarea value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <div className="flex gap-2">
              <Button variant="outline" disabled={saving} onClick={() => save(false)}>
                {t("quiz.admin.saveProgress")}
              </Button>
              <Button disabled={saving} onClick={() => save(true)}>
                {t("quiz.admin.finalize")}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
