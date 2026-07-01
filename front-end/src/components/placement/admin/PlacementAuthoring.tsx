import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  createPlacementQuestion,
  deletePlacementQuestion,
  getPlacementConfig,
  getPlacementLevelThresholds,
  getPlacementQuestions,
  reorderPlacementQuestions,
  updatePlacementConfig,
  updatePlacementLevelThresholds,
  updatePlacementQuestion,
  uploadHomeworkAudio,
  type AdminPlacementOption,
  type AdminPlacementQuestion,
  type ApiError,
  type PlacementConfig,
  type PlacementLevelThreshold,
  type PlacementQuestionKind,
  type PlacementSkill,
  type UpsertPlacementQuestion,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const SKILLS: PlacementSkill[] = ["READING", "LISTENING", "GRAMMAR"];
const KINDS: PlacementQuestionKind[] = [
  "SINGLE_CHOICE",
  "MULTI_CHOICE",
  "FILL_BLANK",
];
const CEFR_LEVELS: PlacementLevelThreshold["level"][] = [
  "A1",
  "A2",
  "B1",
  "B2",
  "C1",
  "C2",
];

function emptyDraft(skill: PlacementSkill): UpsertPlacementQuestion {
  return {
    skill,
    kind: "SINGLE_CHOICE",
    prompt: "",
    audioUrl: null,
    audioFileId: null,
    active: true,
    options: [
      { label: "", isCorrect: true },
      { label: "", isCorrect: false },
    ],
  };
}

function toUpsert(q: AdminPlacementQuestion): UpsertPlacementQuestion {
  return {
    skill: q.skill,
    kind: q.kind,
    prompt: q.prompt,
    audioUrl: q.audioUrl,
    audioFileId: q.audioFileId,
    active: q.active,
    options: q.options.map((o) => ({ label: o.label, isCorrect: o.isCorrect })),
  };
}

function QuestionEditor({
  draft,
  onChange,
}: {
  draft: UpsertPlacementQuestion;
  onChange: (next: UpsertPlacementQuestion) => void;
}) {
  const { t } = useTranslation();
  const [uploading, setUploading] = useState(false);

  const setOption = (index: number, next: AdminPlacementOption) => {
    const options = [...draft.options];
    options[index] = next;
    onChange({ ...draft, options });
  };

  const addOption = () =>
    onChange({
      ...draft,
      options: [...draft.options, { label: "", isCorrect: false }],
    });

  const removeOption = (index: number) =>
    onChange({
      ...draft,
      options: draft.options.filter((_, i) => i !== index),
    });

  const handleAudioUpload = async (file: File) => {
    setUploading(true);
    try {
      const result = await uploadHomeworkAudio(file);
      onChange({ ...draft, audioFileId: result.id, audioUrl: null });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="space-y-4 rounded-lg border p-4">
      <div className="max-w-xs">
        <Label>{t("placement.admin.kind")}</Label>
        <Select
          value={draft.kind}
          onValueChange={(v) =>
            onChange({ ...draft, kind: v as PlacementQuestionKind })
          }
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {KINDS.map((k) => (
              <SelectItem key={k} value={k}>
                {t(`placement.admin.kindOptions.${k}` as never)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div>
        <Label>{t("placement.admin.prompt")}</Label>
        <Textarea
          value={draft.prompt}
          onChange={(e) => onChange({ ...draft, prompt: e.target.value })}
          rows={2}
        />
      </div>

      {draft.skill === "LISTENING" && (
        <div className="grid grid-cols-2 gap-4">
          <div>
            <Label>{t("placement.admin.audioUrl")}</Label>
            <Input
              value={draft.audioUrl ?? ""}
              onChange={(e) =>
                onChange({
                  ...draft,
                  audioUrl: e.target.value || null,
                  audioFileId: null,
                })
              }
            />
          </div>
          <div>
            <Label>{t("placement.admin.audioFile")}</Label>
            <Input
              type="file"
              accept="audio/*"
              disabled={uploading}
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleAudioUpload(file);
              }}
            />
            {draft.audioFileId && (
              <p className="mt-1 text-xs text-muted-foreground">
                {draft.audioFileId}
              </p>
            )}
          </div>
        </div>
      )}

      <div className="space-y-2">
        <Label>{t("placement.admin.options")}</Label>
        {draft.options.map((o, i) => (
          <div key={i} className="flex items-center gap-2">
            <Input
              value={o.label}
              onChange={(e) => setOption(i, { ...o, label: e.target.value })}
              className="flex-1"
            />
            {draft.kind !== "FILL_BLANK" && (
              <label className="flex items-center gap-1 text-xs">
                <Checkbox
                  checked={o.isCorrect}
                  onCheckedChange={(c) =>
                    setOption(i, { ...o, isCorrect: c === true })
                  }
                />
                {t("placement.admin.correct")}
              </label>
            )}
            <Button variant="ghost" size="sm" onClick={() => removeOption(i)}>
              {t("placement.admin.delete")}
            </Button>
          </div>
        ))}
        <Button variant="outline" size="sm" onClick={addOption}>
          {t("placement.admin.addOption")}
        </Button>
      </div>
    </div>
  );
}

type TimeLimitField =
  | "readingTimeSeconds"
  | "listeningTimeSeconds"
  | "grammarTimeSeconds"
  | "writingTimeSeconds";

const TIME_LIMIT_FIELD = {
  READING: "readingTimeSeconds",
  LISTENING: "listeningTimeSeconds",
  GRAMMAR: "grammarTimeSeconds",
} as const satisfies Record<PlacementSkill, TimeLimitField>;

function TimeLimitEditor({ field }: { field: TimeLimitField }) {
  const { t } = useTranslation();
  const [config, setConfig] = useState<PlacementConfig | null>(null);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<"idle" | "saved" | "error">("idle");

  useEffect(() => {
    getPlacementConfig().then(setConfig);
  }, []);

  if (!config) {
    return (
      <p className="mb-6 text-sm text-muted-foreground animate-pulse">
        {t("common.loading")}
      </p>
    );
  }

  const save = async () => {
    setSaving(true);
    setStatus("idle");
    try {
      const saved = await updatePlacementConfig(config);
      setConfig(saved);
      setStatus("saved");
    } catch {
      setStatus("error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mb-6 max-w-xs space-y-2">
      <Label>{t("placement.admin.timeLimitSeconds")}</Label>
      <div className="flex items-center gap-2">
        <Input
          type="number"
          value={config[field]}
          onChange={(e) =>
            setConfig({ ...config, [field]: Number(e.target.value) })
          }
        />
        <Button onClick={save} disabled={saving} size="sm">
          {saving ? t("placement.admin.saving") : t("placement.admin.save")}
        </Button>
      </div>
      {status === "saved" && (
        <p className="text-sm text-green-700">{t("placement.admin.saved")}</p>
      )}
      {status === "error" && (
        <p className="text-sm text-destructive">
          {t("placement.admin.saveError")}
        </p>
      )}
    </div>
  );
}

function SkillQuestions({ skill }: { skill: PlacementSkill }) {
  const { t } = useTranslation();
  const [questions, setQuestions] = useState<AdminPlacementQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [drafts, setDrafts] = useState<Record<string, UpsertPlacementQuestion>>(
    {},
  );
  const [newDraft, setNewDraft] = useState<UpsertPlacementQuestion | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    getPlacementQuestions(skill)
      .then((qs) => {
        setQuestions(qs);
        setDrafts(Object.fromEntries(qs.map((q) => [q.id, toUpsert(q)])));
      })
      .finally(() => setLoading(false));
  };

  useEffect(load, [skill]);

  const save = async (id: string) => {
    setError(null);
    try {
      await updatePlacementQuestion(id, drafts[id]);
      load();
    } catch (e) {
      setError((e as ApiError).message ?? t("placement.admin.validationError"));
    }
  };

  const remove = async (id: string) => {
    await deletePlacementQuestion(id);
    load();
  };

  const move = async (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= questions.length) return;
    const reordered = [...questions];
    [reordered[index], reordered[target]] = [
      reordered[target],
      reordered[index],
    ];
    await reorderPlacementQuestions(
      skill,
      reordered.map((q) => q.id),
    );
    load();
  };

  const createNew = async () => {
    if (!newDraft) return;
    setError(null);
    try {
      await createPlacementQuestion(newDraft);
      setNewDraft(null);
      load();
    } catch (e) {
      setError((e as ApiError).message ?? t("placement.admin.validationError"));
    }
  };

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("common.loading")}
      </p>
    );
  }

  return (
    <div className="space-y-6">
      {error && <p className="text-sm text-destructive">{error}</p>}

      {questions.map((q, i) => (
        <div key={q.id} className="space-y-2">
          <QuestionEditor
            draft={drafts[q.id] ?? toUpsert(q)}
            onChange={(next) =>
              setDrafts((prev) => ({ ...prev, [q.id]: next }))
            }
          />
          <div className="flex gap-2">
            <Button size="sm" onClick={() => save(q.id)}>
              {t("common.save")}
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => move(i, -1)}
              disabled={i === 0}
            >
              {t("placement.admin.moveUp")}
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => move(i, 1)}
              disabled={i === questions.length - 1}
            >
              {t("placement.admin.moveDown")}
            </Button>
            <Button
              size="sm"
              variant="destructive"
              onClick={() => remove(q.id)}
            >
              {t("placement.admin.delete")}
            </Button>
          </div>
        </div>
      ))}

      {newDraft ? (
        <div className="space-y-2">
          <QuestionEditor draft={newDraft} onChange={setNewDraft} />
          <Button size="sm" onClick={createNew}>
            {t("common.save")}
          </Button>
        </div>
      ) : (
        <Button
          variant="outline"
          onClick={() => setNewDraft(emptyDraft(skill))}
        >
          {t("placement.admin.addQuestion")}
        </Button>
      )}
    </div>
  );
}

function WritingPromptEditor() {
  const { t } = useTranslation();
  const [config, setConfig] = useState<PlacementConfig | null>(null);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<"idle" | "saved" | "error">("idle");

  useEffect(() => {
    getPlacementConfig().then(setConfig);
  }, []);

  if (!config) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("common.loading")}
      </p>
    );
  }

  const save = async () => {
    setSaving(true);
    setStatus("idle");
    try {
      const saved = await updatePlacementConfig(config);
      setConfig(saved);
      setStatus("saved");
    } catch {
      setStatus("error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-xl space-y-4">
      <div>
        <Label>{t("placement.admin.writingPrompt")}</Label>
        <Textarea
          value={config.writingPrompt}
          onChange={(e) =>
            setConfig({ ...config, writingPrompt: e.target.value })
          }
          rows={4}
        />
      </div>

      <Button onClick={save} disabled={saving}>
        {saving ? t("placement.admin.saving") : t("placement.admin.save")}
      </Button>
      {status === "saved" && (
        <p className="text-sm text-green-700">{t("placement.admin.saved")}</p>
      )}
      {status === "error" && (
        <p className="text-sm text-destructive">
          {t("placement.admin.saveError")}
        </p>
      )}
    </div>
  );
}

function LevelThresholdsEditor() {
  const { t } = useTranslation();
  const [thresholds, setThresholds] = useState<
    PlacementLevelThreshold[] | null
  >(null);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<"idle" | "saved" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getPlacementLevelThresholds().then(setThresholds);
  }, []);

  if (!thresholds) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("common.loading")}
      </p>
    );
  }

  const setThreshold = (
    level: PlacementLevelThreshold["level"],
    value: number,
  ) =>
    setThresholds((prev) =>
      (prev ?? []).map((t) =>
        t.level === level ? { ...t, minScorePercent: value } : t,
      ),
    );

  const save = async () => {
    setSaving(true);
    setStatus("idle");
    setError(null);
    try {
      const saved = await updatePlacementLevelThresholds(thresholds);
      setThresholds(saved);
      setStatus("saved");
    } catch (e) {
      setStatus("error");
      setError((e as ApiError).message ?? t("placement.admin.saveError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-xl space-y-4">
      <p className="text-sm text-muted-foreground">
        {t("placement.admin.levelThresholdsHint")}
      </p>
      <div className="space-y-2">
        {CEFR_LEVELS.map((level) => (
          <div key={level} className="flex items-center gap-3">
            <Label className="w-10 shrink-0">{level}</Label>
            <Input
              type="number"
              min={0}
              max={100}
              value={
                thresholds.find((t) => t.level === level)?.minScorePercent ?? 0
              }
              onChange={(e) => setThreshold(level, Number(e.target.value))}
              className="max-w-[100px]"
            />
            <span className="text-sm text-muted-foreground">%</span>
          </div>
        ))}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button onClick={save} disabled={saving}>
        {saving ? t("placement.admin.saving") : t("placement.admin.save")}
      </Button>
      {status === "saved" && (
        <p className="text-sm text-green-700">{t("placement.admin.saved")}</p>
      )}
    </div>
  );
}

/** Admin authoring for the placement test: per-skill question banks + config (FR-014). */
export function PlacementAuthoring() {
  const { t } = useTranslation();

  return (
    <div className="space-y-8">
      <div>
        <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          {t("placement.admin.questionsTitle")}
        </h3>
        <Tabs defaultValue="READING">
          <TabsList>
            {SKILLS.map((s) => (
              <TabsTrigger key={s} value={s}>
                {t(`placement.intro.sections.${s}` as never)}
              </TabsTrigger>
            ))}
            <TabsTrigger value="WRITING">
              {t("placement.fullEvaluation.writingTitle")}
            </TabsTrigger>
            <TabsTrigger value="LEVELS">
              {t("placement.admin.levelsTab")}
            </TabsTrigger>
          </TabsList>
          {SKILLS.map((s) => (
            <TabsContent key={s} value={s} className="mt-6">
              <TimeLimitEditor field={TIME_LIMIT_FIELD[s]} />
              <SkillQuestions skill={s} />
            </TabsContent>
          ))}
          <TabsContent value="WRITING" className="mt-6">
            <TimeLimitEditor field="writingTimeSeconds" />
            <WritingPromptEditor />
          </TabsContent>
          <TabsContent value="LEVELS" className="mt-6">
            <LevelThresholdsEditor />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
