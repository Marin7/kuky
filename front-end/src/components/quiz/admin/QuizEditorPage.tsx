import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  createQuiz,
  getAdminQuiz,
  setQuizAssignees,
  updateQuiz,
  type ApiError,
  type QuizAdminQuestion,
  type QuizSkill,
} from "@/lib/admin";
import { getMe } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { StudentMultiSelect } from "@/components/admin/homework/StudentMultiSelect";
import { QuestionEditorCard } from "@/components/admin/homework/QuestionEditorCard";
import { AudioSourceEditor, type AudioSourceValue } from "@/components/admin/homework/AudioSourceEditor";
import { defaultQuestion } from "@/components/admin/homework/questionDefaults";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { QuizAttemptsPanel } from "./QuizAttemptsPanel";

const SKILLS: QuizSkill[] = ["READING", "WRITING", "GRAMMAR", "LISTENING"];

function emptyQuestion(): QuizAdminQuestion {
  return { ...defaultQuestion(), skill: "READING" };
}

export function QuizEditorPage({ quizId }: { quizId?: string }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isEdit = Boolean(quizId);
  const [authChecked, setAuthChecked] = useState(false);
  const [loading, setLoading] = useState(isEdit);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [questions, setQuestions] = useState<QuizAdminQuestion[]>([emptyQuestion()]);
  const [assigneeIds, setAssigneeIds] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMe()
      .then((me) => {
        if (me.role !== "ADMIN") navigate({ to: "/" });
        else setAuthChecked(true);
      })
      .catch(() => navigate({ to: "/cuenta" }));
  }, []);

  useEffect(() => {
    if (!quizId) return;
    getAdminQuiz(quizId)
      .then((q) => {
        setTitle(q.title);
        setDescription(q.description ?? "");
        setQuestions(
          q.questions.length
            ? q.questions.map((item) => ({ ...item, skill: item.skill ?? "READING" }))
            : [emptyQuestion()],
        );
        setAssigneeIds(q.assignees.map((a) => a.id));
      })
      .catch(() => setError(t("quiz.admin.loadError")))
      .finally(() => setLoading(false));
  }, [quizId]);

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      let id = quizId;
      if (!id) {
        const created = await createQuiz(title.trim(), description.trim() || null);
        id = created.id;
      }
      await updateQuiz(id, title.trim(), description.trim() || null, questions);
      if (assigneeIds.length) {
        await setQuizAssignees(id, assigneeIds);
      }
      navigate({ to: "/panel", search: { tab: "quizzes" } as never });
    } catch (e) {
      setError((e as ApiError).message ?? t("quiz.admin.saveError"));
    } finally {
      setSaving(false);
    }
  };

  if (!authChecked || loading) {
    return (
      <p className="mx-auto max-w-3xl px-6 py-12 text-sm text-muted-foreground">
        {t("common.loading")}
      </p>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6 px-6 py-12">
      <Button variant="ghost" onClick={() => navigate({ to: "/panel", search: { tab: "quizzes" } as never })}>
        {t("admin.homework.editor.backToPanel")}
      </Button>
      <h1 className="font-display text-2xl font-semibold">
        {isEdit ? t("quiz.admin.title") : t("quiz.admin.newQuiz")}
      </h1>
      <div className="space-y-2">
        <Label>{t("quiz.admin.titleLabel")}</Label>
        <Input value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="space-y-2">
        <Label>{t("quiz.admin.description")}</Label>
        <Textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
      </div>
      <div className="space-y-4">
        {questions.map((q, i) => (
          <div key={i} className="space-y-3 rounded-lg border p-4">
            <div className="space-y-1">
              <Label>{t("quiz.admin.skill")}</Label>
              <Select
                value={q.skill}
                onValueChange={(skill: QuizSkill) =>
                  setQuestions((prev) =>
                    prev.map((item, idx) => (idx === i ? { ...item, skill } : item)),
                  )
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SKILLS.map((s) => (
                    <SelectItem key={s} value={s}>
                      {t(`quiz.skills.${s}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {q.skill === "LISTENING" && (
              <div className="space-y-1">
                <Label>{t("quiz.admin.listeningMedia")}</Label>
                <AudioSourceEditor
                  value={{
                    mediaSourceKind: q.mediaSourceKind ?? null,
                    audioUrl: q.audioUrl ?? null,
                    audioFileId: q.audioFileId ?? null,
                    audioFileName: null,
                  }}
                  onChange={(audio: AudioSourceValue) =>
                    setQuestions((prev) =>
                      prev.map((item, idx) =>
                        idx === i
                          ? {
                              ...item,
                              mediaSourceKind: audio.mediaSourceKind,
                              audioUrl: audio.audioUrl,
                              audioFileId: audio.audioFileId,
                            }
                          : item,
                      ),
                    )
                  }
                />
              </div>
            )}
            <QuestionEditorCard
              index={i}
              count={questions.length}
              question={q}
              onChange={(updated) =>
                setQuestions((prev) =>
                  prev.map((item, idx) => (idx === i ? { ...updated, skill: item.skill, mediaSourceKind: item.mediaSourceKind, audioUrl: item.audioUrl, audioFileId: item.audioFileId } : item)),
                )
              }
              onRemove={() => setQuestions((prev) => prev.filter((_, idx) => idx !== i))}
              onMoveUp={() => {
                if (i === 0) return;
                setQuestions((prev) => {
                  const next = [...prev];
                  [next[i - 1], next[i]] = [next[i], next[i - 1]];
                  return next;
                });
              }}
              onMoveDown={() => {
                if (i === questions.length - 1) return;
                setQuestions((prev) => {
                  const next = [...prev];
                  [next[i + 1], next[i]] = [next[i], next[i + 1]];
                  return next;
                });
              }}
            />
          </div>
        ))}
        <Button type="button" variant="outline" onClick={() => setQuestions((p) => [...p, emptyQuestion()])}>
          {t("admin.homework.questions.addQuestion")}
        </Button>
      </div>
      <div className="space-y-2">
        <Label>{t("quiz.admin.assign")}</Label>
        <StudentMultiSelect selected={assigneeIds} onChange={setAssigneeIds} />
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      <Button onClick={save} disabled={saving || !title.trim()}>
        {saving ? t("common.saving") : t("common.save")}
      </Button>
      {quizId && <QuizAttemptsPanel quizId={quizId} />}
    </div>
  );
}
