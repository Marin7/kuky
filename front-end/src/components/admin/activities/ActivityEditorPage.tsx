import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  createActivity,
  updateActivity,
  getActivityAdmin,
  listPresentations,
  getPresentation,
  uploadAdminImage,
  type AdminQuestion,
  type HomeworkType,
  type HomeworkLevel,
  type HomeworkFormat,
  type PresentationSummary,
  type PresentationFileSummary,
  type ApiError,
} from "@/lib/admin";
import { getMe } from "@/lib/auth";
import { isPresentationPdf } from "@/lib/learning";
import { extractYoutubeVideoId, activityImageUrl } from "@/lib/youtube";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { QuestionListEditor } from "@/components/admin/homework/QuestionListEditor";
import { Textarea } from "@/components/ui/textarea";

const LEVEL_OPTIONS: HomeworkLevel[] = ["A1", "A2", "B1", "B2", "C1", "C2"];

interface Props {
  activityId?: string;
}

export function ActivityEditorPage({ activityId }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isEdit = Boolean(activityId);

  const [authChecked, setAuthChecked] = useState(false);
  const [loading, setLoading] = useState(isEdit);

  const [title, setTitle] = useState("");
  const [presentationId, setPresentationId] = useState("");
  const [presentations, setPresentations] = useState<PresentationSummary[]>([]);
  const [pdfFiles, setPdfFiles] = useState<PresentationFileSummary[]>([]);
  const [format, setFormat] = useState<HomeworkFormat>("MANUAL");
  const [homeworkType, setHomeworkType] = useState<HomeworkType | "">("");
  const [level, setLevel] = useState<HomeworkLevel | "">("");
  const [questions, setQuestions] = useState<AdminQuestion[]>([]);
  const [triggerFileId, setTriggerFileId] = useState<string>("");
  const [triggerPage, setTriggerPage] = useState<string>("");
  const [instructionsText, setInstructionsText] = useState("");
  const [youtubeUrl, setYoutubeUrl] = useState("");
  const [imageId, setImageId] = useState<string | null>(null);
  const [uploadingImage, setUploadingImage] = useState(false);

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
    listPresentations()
      .then(setPresentations)
      .catch(() => setPresentations([]));
  }, []);

  useEffect(() => {
    if (!presentationId) {
      setPdfFiles([]);
      setTriggerFileId("");
      return;
    }
    getPresentation(presentationId)
      .then((p) => {
        const pdfs = p.files.filter((f) => isPresentationPdf(f.contentType));
        setPdfFiles(pdfs);
        setTriggerFileId((current) => {
          if (pdfs.length === 1) return pdfs[0].id;
          if (current && pdfs.some((f) => f.id === current)) return current;
          return "";
        });
      })
      .catch(() => {
        setPdfFiles([]);
        setTriggerFileId("");
      });
  }, [presentationId]);

  useEffect(() => {
    if (!activityId) return;
    getActivityAdmin(activityId)
      .then((a) => {
        setTitle(a.title);
        setPresentationId(a.presentationId);
        setFormat(a.format);
        setHomeworkType(a.homeworkType ?? "");
        setLevel(a.level ?? "");
        setQuestions(a.questions ?? []);
        setTriggerFileId(a.triggerFileId ?? "");
        setTriggerPage(a.triggerPage != null ? String(a.triggerPage) : "");
        setInstructionsText(a.instructionsText ?? "");
        setYoutubeUrl(a.youtubeUrl ?? "");
        setImageId(a.imageId ?? null);
      })
      .catch(() => setError(t("admin.activities.editor.loadError")))
      .finally(() => setLoading(false));
  }, [activityId, t]);

  const backToList = () =>
    navigate({ to: "/panel", search: { tab: "activities" } as never });

  const save = async () => {
    if (!title.trim() || !presentationId) {
      setError(t("admin.activities.editor.requiredError"));
      return;
    }
    if (!instructionsText.trim()) {
      setError(t("admin.activities.editor.instructionsRequired"));
      return;
    }
    const hasYoutube = Boolean(extractYoutubeVideoId(youtubeUrl));
    if (!hasYoutube && !imageId) {
      setError(t("admin.activities.editor.mediaRequired"));
      return;
    }
    if (youtubeUrl.trim() && !hasYoutube) {
      setError(t("admin.activities.editor.youtubeInvalid"));
      return;
    }
    if (!triggerFileId) {
      setError(
        pdfFiles.length === 0
          ? t("admin.activities.editor.noPdfError")
          : t("admin.activities.editor.triggerFileError"),
      );
      return;
    }
    const pageNum = Number(triggerPage);
    if (!Number.isFinite(pageNum) || pageNum < 1) {
      setError(t("admin.activities.editor.triggerPageError"));
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const fields = {
        title: title.trim(),
        presentationId,
        format,
        level: level || null,
        homeworkType: homeworkType || null,
        triggerFileId,
        triggerPage: pageNum,
        instructionsText: instructionsText.trim(),
        youtubeUrl: youtubeUrl.trim() || null,
        imageId,
        questions: format === "EXERCISE" ? questions : [],
      };
      if (activityId) {
        await updateActivity(activityId, fields);
      } else {
        await createActivity(fields);
      }
      backToList();
    } catch (e) {
      setError(
        (e as ApiError).message ?? t("admin.activities.editor.saveError"),
      );
    } finally {
      setSaving(false);
    }
  };

  if (!authChecked) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("admin.activities.loading")}
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
        {t("admin.activities.editor.backToPanel")}
      </button>

      <h1 className="font-display text-3xl font-semibold text-primary">
        {isEdit
          ? t("admin.activities.editor.editTitle")
          : t("admin.activities.editor.newTitle")}
      </h1>

      {loading ? (
        <p className="mt-8 animate-pulse text-sm text-muted-foreground">
          {t("admin.activities.editor.loading")}
        </p>
      ) : (
        <div className="mt-8 space-y-6">
          <div className="space-y-1">
            <Label htmlFor="act-title">
              {t("admin.activities.editor.titleLabel")}
            </Label>
            <Input
              id="act-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
            />
          </div>

          <div className="space-y-1">
            <Label>{t("admin.activities.presentation")}</Label>
            <Select value={presentationId} onValueChange={setPresentationId}>
              <SelectTrigger>
                <SelectValue
                  placeholder={t("admin.activities.editor.presentationPlaceholder")}
                />
              </SelectTrigger>
              <SelectContent>
                {presentations.map((p) => (
                  <SelectItem key={p.id} value={p.id}>
                    {p.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
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
          </div>

          <div className="space-y-2">
            <Label>{t("admin.homework.editor.formatLabel")}</Label>
            <RadioGroup
              value={format}
              onValueChange={(v) => setFormat(v as HomeworkFormat)}
              className="flex flex-col gap-2"
            >
              <label className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="MANUAL" id="act-fmt-manual" />
                {t("admin.homework.editor.formatManual")}
              </label>
              <label className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="EXERCISE" id="act-fmt-exercise" />
                {t("admin.homework.editor.formatExercise")}
              </label>
            </RadioGroup>
          </div>

          {format === "EXERCISE" && (
            <div className="rounded-lg border bg-muted/30 p-4">
              <QuestionListEditor
                questions={questions}
                onChange={setQuestions}
              />
            </div>
          )}

          <div className="space-y-1">
            <Label htmlFor="act-instructions">
              {t("admin.activities.instructionsText")}
            </Label>
            <Textarea
              id="act-instructions"
              value={instructionsText}
              onChange={(e) => setInstructionsText(e.target.value)}
              rows={5}
            />
          </div>

          <div className="space-y-1">
            <Label htmlFor="act-youtube">
              {t("admin.activities.youtubeUrl")}
            </Label>
            <Input
              id="act-youtube"
              type="url"
              value={youtubeUrl}
              onChange={(e) => setYoutubeUrl(e.target.value)}
              placeholder={t("admin.activities.youtubePlaceholder")}
            />
            <p className="text-xs text-muted-foreground">
              {t("admin.activities.youtubeHint")}
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="act-image">{t("admin.activities.photo")}</Label>
            <Input
              id="act-image"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              disabled={uploadingImage || saving}
              onChange={async (e) => {
                const file = e.target.files?.[0];
                e.target.value = "";
                if (!file) return;
                setUploadingImage(true);
                setError(null);
                try {
                  const uploaded = await uploadAdminImage(file);
                  setImageId(uploaded.id);
                } catch (err) {
                  setError(
                    (err as ApiError).message ??
                      t("admin.activities.editor.imageUploadError"),
                  );
                } finally {
                  setUploadingImage(false);
                }
              }}
            />
            <p className="text-xs text-muted-foreground">
              {t("admin.activities.photoHint")}
            </p>
            {imageId && (
              <div className="space-y-2">
                <img
                  src={activityImageUrl(imageId)}
                  alt=""
                  className="max-h-48 rounded-md border object-contain"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setImageId(null)}
                >
                  {t("admin.activities.removePhoto")}
                </Button>
              </div>
            )}
            {uploadingImage && (
              <p className="text-xs text-muted-foreground">
                {t("admin.activities.editor.uploadingImage")}
              </p>
            )}
          </div>

          <div className="space-y-3 rounded-lg border p-4">
            <Label>{t("admin.activities.trigger")}</Label>
            <p className="text-xs text-muted-foreground">
              {t("admin.activities.triggerHint")}
            </p>
            <div
              className={
                pdfFiles.length > 1
                  ? "grid gap-4 sm:grid-cols-2"
                  : "grid gap-4"
              }
            >
              {pdfFiles.length > 1 && (
                <div className="space-y-1">
                  <Label>{t("admin.activities.triggerFile")}</Label>
                  <Select
                    value={triggerFileId || undefined}
                    onValueChange={setTriggerFileId}
                    disabled={!presentationId}
                  >
                    <SelectTrigger>
                      <SelectValue
                        placeholder={t(
                          "admin.activities.triggerFilePlaceholder",
                        )}
                      />
                    </SelectTrigger>
                    <SelectContent>
                      {pdfFiles.map((f) => (
                        <SelectItem key={f.id} value={f.id}>
                          {f.displayName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
              <div className="space-y-1">
                <Label htmlFor="act-trigger-page">
                  {t("admin.activities.triggerPage")}
                </Label>
                <Input
                  id="act-trigger-page"
                  type="number"
                  min={1}
                  value={triggerPage}
                  onChange={(e) => setTriggerPage(e.target.value)}
                  disabled={!presentationId || pdfFiles.length === 0}
                />
              </div>
            </div>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={backToList} disabled={saving}>
              {t("admin.activities.cancel")}
            </Button>
            <Button onClick={save} disabled={saving}>
              {saving
                ? t("admin.activities.editor.saving")
                : t("admin.activities.save")}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
