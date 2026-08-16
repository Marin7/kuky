import { useEffect, useState, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  createHomework,
  updateHomework,
  updateHomeworkLabels,
  setAssignees,
  getHomework,
  getHomeworkById,
  type AdminQuestion,
  type Assignee,
  type HomeworkType,
  type HomeworkLevel,
  type ApiError,
} from "@/lib/admin";
import { getMe } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { StudentMultiSelect } from "./StudentMultiSelect";
import { QuestionListEditor } from "./QuestionListEditor";
import { AudioSourceEditor, type AudioSourceValue } from "./AudioSourceEditor";
import { HomeworkLabelField } from "./HomeworkLabelField";
import {
  uniqueLabels,
  homeworkLabels,
  labelsEqual,
} from "@/lib/homeworkLabels";
import { HomeworkAssigneeList } from "./HomeworkAssigneeList";
import { HomeworkReviewDialog } from "./HomeworkReviewDialog";
import { ExerciseResultDialog } from "./ExerciseResultDialog";
import { notifyBadgesChanged } from "@/lib/notifications";

const LEVEL_OPTIONS: HomeworkLevel[] = ["A1", "A2", "B1", "B2", "C1", "C2"];

interface Props {
  homeworkId?: string;
}

export function HomeworkEditorPage({ homeworkId }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isEdit = Boolean(homeworkId);

  const [authChecked, setAuthChecked] = useState(false);
  const [loading, setLoading] = useState(isEdit);

  const [title, setTitle] = useState("");
  const [instructions, setInstructions] = useState("");
  const [dueOn, setDueOn] = useState("");
  const [homeworkType, setHomeworkType] = useState<HomeworkType | "">("");
  const [level, setLevel] = useState<HomeworkLevel | "">("");
  const [labels, setLabels] = useState<string[]>([]);
  const savedLabelsRef = useRef<string[]>([]);
  const [existingLabels, setExistingLabels] = useState<string[]>([]);
  const [questions, setQuestions] = useState<AdminQuestion[]>([]);
  const [audio, setAudio] = useState<AudioSourceValue>({
    mediaSourceKind: null,
    audioUrl: null,
    audioFileId: null,
    audioFileName: null,
  });
  const [assigneeIds, setAssigneeIds] = useState<string[]>([]);
  const [assignees, setAssigneeRows] = useState<Assignee[]>([]);
  const [openSubmissionId, setOpenSubmissionId] = useState<string | null>(null);
  const [openResultId, setOpenResultId] = useState<string | null>(null);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reloadAssignees = () => {
    if (!homeworkId) return;
    getHomeworkById(homeworkId)
      .then((hw) => {
        setAssigneeRows(hw.assignees);
        setAssigneeIds(hw.assignees.map((a) => a.userId));
      })
      .catch(() => {});
  };

  useEffect(() => {
    getMe()
      .then((me) => {
        if (me.role !== "ADMIN") navigate({ to: "/" });
        else setAuthChecked(true);
      })
      .catch(() => navigate({ to: "/cuenta" }));
  }, []);

  useEffect(() => {
    if (!homeworkId) return;
    getHomeworkById(homeworkId)
      .then((hw) => {
        setTitle(hw.title);
        setInstructions(hw.instructions);
        setDueOn(hw.dueOn ?? "");
        setHomeworkType(hw.homeworkType ?? "");
        setLevel(hw.level ?? "");
        setLabels(homeworkLabels(hw));
        savedLabelsRef.current = homeworkLabels(hw);
        setQuestions(hw.questions ?? []);
        setAudio({
          mediaSourceKind: hw.mediaSourceKind,
          audioUrl: hw.audioUrl,
          audioFileId: hw.audioFileId,
          audioFileName: hw.audioFileName,
        });
        setAssigneeRows(hw.assignees);
        setAssigneeIds(hw.assignees.map((a) => a.userId));
      })
      .catch(() => setError(t("admin.homework.editor.loadError")))
      .finally(() => setLoading(false));
  }, [homeworkId]);

  useEffect(() => {
    getHomework()
      .then((list) => setExistingLabels(uniqueLabels(list)))
      .catch(() => setExistingLabels([]));
  }, []);

  // Writing homework is a single rich-text answer — no question list.
  useEffect(() => {
    if (homeworkType === "WRITE") setQuestions([]);
  }, [homeworkType]);

  const backToList = () =>
    navigate({ to: "/panel", search: { tab: "homework" } as never });

  const persistLabels = async (next: string[]) => {
    const previous = savedLabelsRef.current;
    if (labelsEqual(next, previous)) return;
    savedLabelsRef.current = next;
    setLabels(next);
    if (!homeworkId) return;
    try {
      await updateHomeworkLabels(homeworkId, next);
      const list = await getHomework();
      setExistingLabels(uniqueLabels(list));
    } catch (e) {
      savedLabelsRef.current = previous;
      setLabels(previous);
      setError((e as ApiError).message ?? t("admin.homework.editor.saveError"));
    }
  };

  const save = async () => {
    if (!title.trim() || !instructions.trim()) {
      setError(t("admin.homework.editor.requiredError"));
      return;
    }
    const type = homeworkType || null;
    if (type !== "WRITE" && questions.length === 0) {
      setError(t("admin.homework.editor.questionsRequired"));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const due = dueOn ? dueOn : null;
      const lvl = level || null;
      const qs = type === "WRITE" ? [] : questions;
      // Audio is only meaningful for listening homework; clear it otherwise.
      const audioPayload =
        type === "AUDIO"
          ? {
              mediaSourceKind: audio.mediaSourceKind,
              audioUrl: audio.audioUrl?.trim() ? audio.audioUrl.trim() : null,
              audioFileId: audio.audioFileId,
            }
          : {
              mediaSourceKind: null,
              audioUrl: null,
              audioFileId: null,
            };
      if (type === "AUDIO") {
        const kind = audio.mediaSourceKind;
        const hasFile = kind === "UPLOADED_FILE" && !!audio.audioFileId;
        const hasUrl =
          (kind === "AUDIO_URL" ||
            kind === "VIDEO_PAGE" ||
            kind === "YOUTUBE") &&
          !!audio.audioUrl?.trim();
        if (!kind || (!hasFile && !hasUrl)) {
          setError(t("admin.homework.editor.audioRequired"));
          setSaving(false);
          return;
        }
      }
      if (homeworkId) {
        await updateHomework(
          homeworkId,
          title,
          instructions,
          due,
          type,
          lvl,
          qs,
          audioPayload,
          labels,
        );
        await setAssignees(homeworkId, assigneeIds);
      } else {
        await createHomework(
          title,
          instructions,
          due,
          type,
          lvl,
          qs,
          audioPayload,
          assigneeIds,
          labels,
        );
      }
      backToList();
    } catch (e) {
      setError((e as ApiError).message ?? t("admin.homework.editor.saveError"));
    } finally {
      setSaving(false);
    }
  };

  if (!authChecked) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("admin.homework.loading")}
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <button
        type="button"
        onClick={backToList}
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("admin.homework.editor.backToPanel")}
      </button>

      <h1 className="font-display text-3xl font-semibold text-primary">
        {isEdit
          ? t("admin.homework.editor.editTitle")
          : t("admin.homework.editor.newTitle")}
      </h1>

      {loading ? (
        <p className="mt-8 animate-pulse text-sm text-muted-foreground">
          {t("admin.homework.editor.loading")}
        </p>
      ) : (
        <div className="mt-8 space-y-6">
          <div className="space-y-1">
            <Label htmlFor="hw-title">
              {t("admin.homework.editor.titleLabel")}
            </Label>
            <Input
              id="hw-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
            />
          </div>

          <div className="space-y-1">
            <Label htmlFor="hw-instructions">
              {t("admin.homework.editor.instructionsLabel")}
            </Label>
            <Textarea
              id="hw-instructions"
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              rows={4}
              maxLength={5000}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-1">
              <Label>{t("admin.homework.editor.typeLabel")}</Label>
              <Select
                value={homeworkType}
                onValueChange={(v) => setHomeworkType(v as HomeworkType)}
              >
                <SelectTrigger>
                  <SelectValue
                    placeholder={t("admin.homework.editor.typePlaceholder")}
                  />
                </SelectTrigger>
                <SelectContent>
                  {(
                    ["AUDIO", "READ", "WRITE", "GRAMMAR"] as HomeworkType[]
                  ).map((v) => (
                    <SelectItem key={v} value={v}>
                      {t(`admin.homework.type.${v}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>{t("admin.homework.editor.levelLabel")}</Label>
              <Select
                value={level}
                onValueChange={(v) => setLevel(v as HomeworkLevel)}
              >
                <SelectTrigger>
                  <SelectValue
                    placeholder={t("admin.homework.editor.levelPlaceholder")}
                  />
                </SelectTrigger>
                <SelectContent>
                  {LEVEL_OPTIONS.map((l) => (
                    <SelectItem key={l} value={l}>
                      {l}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>{t("admin.homework.editor.labelLabel")}</Label>
              <HomeworkLabelField
                value={labels}
                onChange={persistLabels}
                existing={existingLabels}
              />
              <p className="text-xs text-muted-foreground">
                {homeworkId
                  ? t("admin.homework.editor.labelSavesImmediately")
                  : t("admin.homework.editor.labelSavesOnCreate")}
              </p>
            </div>
            <div className="space-y-1">
              <Label htmlFor="hw-due">
                {t("admin.homework.editor.dueDateLabel")}
              </Label>
              <Input
                id="hw-due"
                type="date"
                value={dueOn}
                onChange={(e) => setDueOn(e.target.value)}
              />
            </div>
          </div>

          {homeworkType === "WRITE" && (
            <p className="text-xs text-muted-foreground">
              {t("admin.homework.editor.writeHint")}
            </p>
          )}

          {homeworkType === "AUDIO" && (
            <AudioSourceEditor value={audio} onChange={setAudio} />
          )}

          {homeworkType !== "WRITE" && (
            <div className="rounded-lg border bg-muted/30 p-4">
              <QuestionListEditor
                questions={questions}
                onChange={setQuestions}
              />
            </div>
          )}

          <div className="space-y-1">
            <Label>{t("admin.homework.editor.assignLabel")}</Label>
            <StudentMultiSelect
              selected={assigneeIds}
              onChange={setAssigneeIds}
            />
          </div>

          {isEdit && assignees.length > 0 && (
            <div className="space-y-2 rounded-lg border p-3">
              <p className="text-sm font-medium">
                {t("admin.homework.editor.assignLabel")}
              </p>
              <HomeworkAssigneeList
                assignees={assignees}
                onOpenResult={setOpenResultId}
                onOpenReview={setOpenSubmissionId}
              />
            </div>
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={backToList} disabled={saving}>
              {t("admin.homework.editor.cancel")}
            </Button>
            <Button onClick={save} disabled={saving}>
              {saving
                ? t("admin.homework.editor.saving")
                : t("admin.homework.editor.save")}
            </Button>
          </div>
        </div>
      )}

      {openSubmissionId && (
        <HomeworkReviewDialog
          submissionId={openSubmissionId}
          onClose={() => setOpenSubmissionId(null)}
          onReviewed={() => {
            setOpenSubmissionId(null);
            reloadAssignees();
            notifyBadgesChanged();
          }}
        />
      )}
      {openResultId && (
        <ExerciseResultDialog
          submissionId={openResultId}
          onClose={() => {
            setOpenResultId(null);
            reloadAssignees();
            notifyBadgesChanged();
          }}
          onFeedbackSaved={() => {
            reloadAssignees();
            notifyBadgesChanged();
          }}
        />
      )}
    </div>
  );
}
