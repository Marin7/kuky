import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { uploadHomeworkAudio, type ApiError } from "@/lib/admin";
import { AudioPlayer } from "@/components/learning/AudioPlayer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

export interface AudioSourceValue {
  audioUrl: string | null;
  audioFileId: string | null;
  audioFileName: string | null;
}

interface Props {
  value: AudioSourceValue;
  onChange: (next: AudioSourceValue) => void;
}

/**
 * Audio-source picker for listening homework: the teacher either pastes an
 * external URL (YouTube, Vimeo, a direct audio link) or uploads an audio file.
 * Only one source is kept at a time.
 */
export function AudioSourceEditor({ value, onChange }: Props) {
  const { t } = useTranslation();
  const [mode, setMode] = useState<"url" | "file">(
    value.audioFileId ? "file" : "url",
  );
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const switchMode = (next: "url" | "file") => {
    setMode(next);
    setError(null);
    // Switching source mode clears the other source so only one is ever set.
    if (next === "url") {
      onChange({ ...value, audioFileId: null, audioFileName: null });
    } else {
      onChange({ ...value, audioUrl: null });
    }
  };

  const handleUpload = async (file: File | undefined) => {
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const up = await uploadHomeworkAudio(file);
      onChange({
        audioUrl: null,
        audioFileId: up.id,
        audioFileName: up.originalName,
      });
    } catch (e) {
      setError(
        (e as ApiError).message ?? t("admin.homework.editor.audioUploadError"),
      );
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  return (
    <div className="space-y-3 rounded-lg border bg-muted/30 p-4">
      <div className="space-y-1">
        <Label>{t("admin.homework.editor.audioLabel")}</Label>
        <p className="text-xs text-muted-foreground">
          {t("admin.homework.editor.audioHint")}
        </p>
      </div>

      <RadioGroup
        value={mode}
        onValueChange={(v) => switchMode(v as "url" | "file")}
        className="flex flex-wrap gap-4"
      >
        <label className="flex items-center gap-2 text-sm">
          <RadioGroupItem value="url" id="audio-mode-url" />
          {t("admin.homework.editor.audioModeUrl")}
        </label>
        <label className="flex items-center gap-2 text-sm">
          <RadioGroupItem value="file" id="audio-mode-file" />
          {t("admin.homework.editor.audioModeFile")}
        </label>
      </RadioGroup>

      {mode === "url" ? (
        <Input
          type="url"
          value={value.audioUrl ?? ""}
          onChange={(e) =>
            onChange({
              audioUrl: e.target.value,
              audioFileId: null,
              audioFileName: null,
            })
          }
          placeholder={t("admin.homework.editor.audioUrlPlaceholder")}
        />
      ) : (
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <input
              ref={fileInputRef}
              type="file"
              accept="audio/*"
              className="hidden"
              onChange={(e) => handleUpload(e.target.files?.[0])}
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={uploading}
              onClick={() => fileInputRef.current?.click()}
            >
              {uploading
                ? t("admin.homework.editor.audioUploading")
                : t("admin.homework.editor.audioUpload")}
            </Button>
            {value.audioFileName && (
              <>
                <span className="text-sm text-muted-foreground">
                  {value.audioFileName}
                </span>
                <button
                  type="button"
                  onClick={() =>
                    onChange({
                      audioUrl: null,
                      audioFileId: null,
                      audioFileName: null,
                    })
                  }
                  className="text-xs text-destructive underline hover:opacity-80"
                >
                  {t("admin.homework.editor.audioRemove")}
                </button>
              </>
            )}
          </div>
          <p className="text-xs text-muted-foreground">
            {t("admin.homework.editor.audioFormatsHint")}
          </p>
        </div>
      )}

      {error && <p className="text-sm text-destructive">{error}</p>}

      {(value.audioUrl || value.audioFileId) && (
        <div className="pt-1">
          <AudioPlayer
            audioUrl={value.audioUrl}
            audioFileId={value.audioFileId}
          />
        </div>
      )}
    </div>
  );
}
