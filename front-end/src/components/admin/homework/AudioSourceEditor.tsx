import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  uploadHomeworkAudio,
  type ApiError,
  type MediaSourceKind,
} from "@/lib/admin";
import { AudioPlayer } from "@/components/learning/AudioPlayer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

export interface AudioSourceValue {
  mediaSourceKind: MediaSourceKind | null;
  audioUrl: string | null;
  audioFileId: string | null;
  audioFileName: string | null;
}

interface Props {
  value: AudioSourceValue;
  onChange: (next: AudioSourceValue) => void;
}

/** Matches YouTube watch / short / embed / youtu.be forms (same as AudioPlayer). */
function youTubeId(url: string): string | null {
  const m = url.match(
    /(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/)|youtu\.be\/)([\w-]{11})/,
  );
  return m ? m[1] : null;
}

const MODES: MediaSourceKind[] = [
  "AUDIO_URL",
  "UPLOADED_FILE",
  "VIDEO_PAGE",
  "YOUTUBE",
];

/**
 * Listening media picker: audio URL, uploaded file, outbound video-page link,
 * or YouTube embed. Only one source is kept at a time.
 */
export function AudioSourceEditor({ value, onChange }: Props) {
  const { t } = useTranslation();
  const [mode, setMode] = useState<MediaSourceKind>(
    value.mediaSourceKind ?? "AUDIO_URL",
  );
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Sync radio from stored kind when loading an existing homework (never infer YouTube).
  useEffect(() => {
    if (value.mediaSourceKind) {
      setMode(value.mediaSourceKind);
    }
  }, [value.mediaSourceKind]);

  const switchMode = (next: MediaSourceKind) => {
    setMode(next);
    setError(null);
    if (next === "UPLOADED_FILE") {
      onChange({
        mediaSourceKind: next,
        audioUrl: null,
        audioFileId: value.audioFileId,
        audioFileName: value.audioFileName,
      });
    } else {
      onChange({
        mediaSourceKind: next,
        audioUrl: value.audioUrl,
        audioFileId: null,
        audioFileName: null,
      });
    }
  };

  const applyUrl = (raw: string, currentMode: MediaSourceKind) => {
    const trimmed = raw.trim();
    // Auto-switch to YouTube when pasting into Enlace or video-page (not on legacy load).
    if (
      (currentMode === "AUDIO_URL" || currentMode === "VIDEO_PAGE") &&
      trimmed &&
      youTubeId(trimmed)
    ) {
      setMode("YOUTUBE");
      setError(null);
      onChange({
        mediaSourceKind: "YOUTUBE",
        audioUrl: trimmed,
        audioFileId: null,
        audioFileName: null,
      });
      return;
    }
    onChange({
      mediaSourceKind: currentMode,
      audioUrl: raw.length ? raw : null,
      audioFileId: null,
      audioFileName: null,
    });
  };

  const handleUpload = async (file: File | undefined) => {
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const up = await uploadHomeworkAudio(file);
      onChange({
        mediaSourceKind: "UPLOADED_FILE",
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

  const urlPlaceholder =
    mode === "YOUTUBE"
      ? t("admin.homework.editor.audioYoutubePlaceholder")
      : mode === "VIDEO_PAGE"
        ? t("admin.homework.editor.audioVideoPagePlaceholder")
        : t("admin.homework.editor.audioUrlPlaceholder");

  const hasPreview =
    (mode === "UPLOADED_FILE" && value.audioFileId) ||
    ((mode === "AUDIO_URL" || mode === "VIDEO_PAGE" || mode === "YOUTUBE") &&
      value.audioUrl);

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
        onValueChange={(v) => switchMode(v as MediaSourceKind)}
        className="flex flex-wrap gap-4"
      >
        {MODES.map((m) => (
          <label key={m} className="flex items-center gap-2 text-sm">
            <RadioGroupItem value={m} id={`audio-mode-${m}`} />
            {t(`admin.homework.editor.audioMode.${m}`)}
          </label>
        ))}
      </RadioGroup>

      {mode === "UPLOADED_FILE" ? (
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
                      mediaSourceKind: "UPLOADED_FILE",
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
      ) : (
        <Input
          type="url"
          value={value.audioUrl ?? ""}
          onChange={(e) => applyUrl(e.target.value, mode)}
          placeholder={urlPlaceholder}
        />
      )}

      {error && <p className="text-sm text-destructive">{error}</p>}

      {hasPreview && (
        <div className="pt-1">
          <AudioPlayer
            mediaSourceKind={mode}
            audioUrl={value.audioUrl}
            audioFileId={value.audioFileId}
          />
        </div>
      )}
    </div>
  );
}
