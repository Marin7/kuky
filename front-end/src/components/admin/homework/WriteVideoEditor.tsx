import { useTranslation } from "react-i18next";
import { AudioPlayer, youTubeId } from "@/components/learning/AudioPlayer";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Props {
  /** Current YouTube URL, or null/empty when no video is set. */
  value: string | null;
  onChange: (url: string) => void;
}

/**
 * Optional YouTube prompt video for writing homework. Unlike the four-way
 * listening AudioSourceEditor, this is a single URL field — YouTube only,
 * and the video is never required.
 */
export function WriteVideoEditor({ value, onChange }: Props) {
  const { t } = useTranslation();
  const url = value ?? "";
  const trimmed = url.trim();
  const hasValue = trimmed.length > 0;
  const isValid = !hasValue || youTubeId(trimmed) != null;

  return (
    <div className="space-y-3 rounded-lg border bg-muted/30 p-4">
      <div className="space-y-1">
        <Label htmlFor="write-video-url">
          {t("admin.homework.editor.writeVideoLabel")}
        </Label>
        <p className="text-xs text-muted-foreground">
          {t("admin.homework.editor.writeVideoHint")}
        </p>
      </div>

      <Input
        id="write-video-url"
        type="url"
        value={url}
        onChange={(e) => onChange(e.target.value)}
        placeholder={t("admin.homework.editor.writeVideoPlaceholder")}
      />

      {!isValid && (
        <p className="text-sm text-destructive">
          {t("admin.homework.editor.writeVideoInvalid")}
        </p>
      )}

      {hasValue && isValid && (
        <div className="pt-1">
          <AudioPlayer
            mediaSourceKind="YOUTUBE"
            audioUrl={trimmed}
            audioFileId={null}
          />
        </div>
      )}
    </div>
  );
}
