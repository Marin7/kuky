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

type SkillBlock = { skill: QuizSkill; start: number; count: number };

function emptyQuestion(skill: QuizSkill = "READING"): QuizAdminQuestion {
  return { ...defaultQuestion(), skill };
}

function nextSkill(after: QuizSkill): QuizSkill {
  const i = SKILLS.indexOf(after);
  return SKILLS[(i + 1) % SKILLS.length];
}

function skillBlocks(questions: QuizAdminQuestion[]): SkillBlock[] {
  const blocks: SkillBlock[] = [];
  for (let i = 0; i < questions.length; i++) {
    const skill = questions[i].skill ?? "READING";
    const last = blocks[blocks.length - 1];
    if (last && last.skill === skill) last.count++;
    else blocks.push({ skill, start: i, count: 1 });
  }
  return blocks;
}

function spliceRange<T>(list: T[], start: number, count: number, insert: T[]): T[] {
  return [...list.slice(0, start), ...insert, ...list.slice(start + count)];
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

  const setBlockSkill = (blockIndex: number, skill: QuizSkill) => {
    setQuestions((prev) => {
      const block = skillBlocks(prev)[blockIndex];
      if (!block) return prev;
      return prev.map((item, idx) => {
        if (idx < block.start || idx >= block.start + block.count) return item;
        if (skill === "LISTENING") return { ...item, skill };
        return {
          ...item,
          skill,
          mediaSourceKind: null,
          audioUrl: null,
          audioFileId: null,
        };
      });
    });
  };

  const addQuestionToBlock = (blockIndex: number) => {
    setQuestions((prev) => {
      const block = skillBlocks(prev)[blockIndex];
      if (!block) return prev;
      const insertAt = block.start + block.count;
      return [...prev.slice(0, insertAt), emptyQuestion(block.skill), ...prev.slice(insertAt)];
    });
  };

  const addSkillBlock = () => {
    setQuestions((prev) => {
      const last = prev[prev.length - 1];
      return [...prev, emptyQuestion(nextSkill(last?.skill ?? "READING"))];
    });
  };

  const removeSkillBlock = (blockIndex: number) => {
    setQuestions((prev) => {
      const block = skillBlocks(prev)[blockIndex];
      if (!block) return prev;
      const next = prev.filter(
        (_, idx) => idx < block.start || idx >= block.start + block.count,
      );
      return next.length ? next : [emptyQuestion()];
    });
  };

  const moveSkillBlock = (blockIndex: number, direction: -1 | 1) => {
    setQuestions((prev) => {
      const blocks = skillBlocks(prev);
      const other = blockIndex + direction;
      if (other < 0 || other >= blocks.length) return prev;
      const a = direction < 0 ? blocks[other] : blocks[blockIndex];
      const b = direction < 0 ? blocks[blockIndex] : blocks[other];
      const first = prev.slice(a.start, a.start + a.count);
      const second = prev.slice(b.start, b.start + b.count);
      return spliceRange(prev, a.start, a.count + b.count, [...second, ...first]);
    });
  };

  if (!authChecked || loading) {
    return (
      <p className="mx-auto max-w-3xl px-6 py-12 text-sm text-muted-foreground">
        {t("common.loading")}
      </p>
    );
  }

  const blocks = skillBlocks(questions);

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
        {blocks.map((block, blockIndex) => (
          <section key={blockIndex} className="space-y-3 rounded-lg border p-4">
            <div className="flex flex-wrap items-end justify-between gap-2">
              <div className="min-w-[12rem] flex-1 space-y-1.5">
                <Label className="font-display text-lg font-semibold text-primary">
                  {t("quiz.admin.skill")}
                </Label>
                <Select
                  value={block.skill}
                  onValueChange={(skill: QuizSkill) => setBlockSkill(blockIndex, skill)}
                >
                  <SelectTrigger className="h-11 text-base font-semibold">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SKILLS.map((s) => (
                      <SelectItem key={s} value={s} className="text-base font-medium">
                        {t(`quiz.skills.${s}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="flex items-center gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-7 px-2 text-xs"
                  disabled={blockIndex === 0}
                  onClick={() => moveSkillBlock(blockIndex, -1)}
                >
                  ↑
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-7 px-2 text-xs"
                  disabled={blockIndex === blocks.length - 1}
                  onClick={() => moveSkillBlock(blockIndex, 1)}
                >
                  ↓
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-7 px-2 text-xs text-destructive"
                  onClick={() => removeSkillBlock(blockIndex)}
                >
                  {t("quiz.admin.removeSkill")}
                </Button>
              </div>
            </div>
            {Array.from({ length: block.count }, (_, offset) => {
              const i = block.start + offset;
              const q = questions[i];
              return (
                <div key={q.id ?? `new-${i}`} className="space-y-3">
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
                    moveUpDisabled={offset === 0}
                    moveDownDisabled={offset === block.count - 1}
                    onChange={(updated) =>
                      setQuestions((prev) =>
                        prev.map((item, idx) =>
                          idx === i
                            ? {
                                ...updated,
                                skill: item.skill,
                                mediaSourceKind: item.mediaSourceKind,
                                audioUrl: item.audioUrl,
                                audioFileId: item.audioFileId,
                              }
                            : item,
                        ),
                      )
                    }
                    onRemove={() =>
                      setQuestions((prev) => {
                        const next = prev.filter((_, idx) => idx !== i);
                        return next.length ? next : [emptyQuestion(block.skill)];
                      })
                    }
                    onMoveUp={() => {
                      if (offset === 0) return;
                      setQuestions((prev) => {
                        const next = [...prev];
                        [next[i - 1], next[i]] = [next[i], next[i - 1]];
                        return next;
                      });
                    }}
                    onMoveDown={() => {
                      if (offset === block.count - 1) return;
                      setQuestions((prev) => {
                        const next = [...prev];
                        [next[i + 1], next[i]] = [next[i], next[i + 1]];
                        return next;
                      });
                    }}
                  />
                </div>
              );
            })}
            <Button type="button" variant="outline" onClick={() => addQuestionToBlock(blockIndex)}>
              {t("admin.homework.questions.addQuestion")}
            </Button>
          </section>
        ))}
        <Button type="button" variant="outline" onClick={addSkillBlock}>
          {t("quiz.admin.addSkill")}
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
