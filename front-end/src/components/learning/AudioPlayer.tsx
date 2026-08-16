import { useTranslation } from "react-i18next";
import { audioFileUrl, type MediaSourceKind } from "@/lib/learning";

interface Props {
  audioUrl: string | null;
  audioFileId: string | null;
  /** When set, drives rendering (VIDEO_PAGE = outbound only; YOUTUBE = embed). */
  mediaSourceKind?: MediaSourceKind | null;
}

/** Extracts a YouTube video id from the common watch / short / embed URL forms. */
export function youTubeId(url: string): string | null {
  const m = url.match(
    /(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/)|youtu\.be\/)([\w-]{11})/,
  );
  return m ? m[1] : null;
}

function vimeoId(url: string): string | null {
  const m = url.match(/vimeo\.com\/(?:video\/)?(\d+)/);
  return m ? m[1] : null;
}

/**
 * Renders listening homework media. Kind selects presentation; legacy
 * AUDIO_URL without kind still auto-embeds YouTube/Vimeo when detected.
 */
export function AudioPlayer({ audioUrl, audioFileId, mediaSourceKind }: Props) {
  const { t } = useTranslation();

  if (mediaSourceKind === "VIDEO_PAGE") {
    if (!audioUrl) return null;
    return (
      <a
        href={audioUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="inline-flex text-sm font-medium text-primary underline hover:opacity-90"
      >
        {t("learning.audioPlayer.openVideoPage")}
      </a>
    );
  }

  if (mediaSourceKind === "YOUTUBE") {
    const yt = audioUrl ? youTubeId(audioUrl) : null;
    if (!yt) return null;
    return (
      <div className="aspect-video w-full overflow-hidden rounded-lg border">
        <iframe
          className="h-full w-full"
          src={`https://www.youtube.com/embed/${yt}`}
          title={t("learning.audioPlayer.title")}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
        />
      </div>
    );
  }

  if (mediaSourceKind === "UPLOADED_FILE" || audioFileId) {
    if (!audioFileId) return null;
    return (
      <audio
        controls
        preload="none"
        src={audioFileUrl(audioFileId)}
        className="w-full"
      >
        {t("learning.audioPlayer.unsupported")}
      </audio>
    );
  }

  if (!audioUrl) return null;

  // AUDIO_URL (or legacy null kind with url): keep YouTube/Vimeo auto-embed.
  const yt = youTubeId(audioUrl);
  if (yt) {
    return (
      <div className="aspect-video w-full overflow-hidden rounded-lg border">
        <iframe
          className="h-full w-full"
          src={`https://www.youtube.com/embed/${yt}`}
          title={t("learning.audioPlayer.title")}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
        />
      </div>
    );
  }

  const vm = vimeoId(audioUrl);
  if (vm) {
    return (
      <div className="aspect-video w-full overflow-hidden rounded-lg border">
        <iframe
          className="h-full w-full"
          src={`https://player.vimeo.com/video/${vm}`}
          title={t("learning.audioPlayer.title")}
          allow="autoplay; fullscreen; picture-in-picture"
          allowFullScreen
        />
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <audio controls preload="none" src={audioUrl} className="w-full">
        {t("learning.audioPlayer.unsupported")}
      </audio>
      <a
        href={audioUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="inline-block text-xs text-muted-foreground underline hover:text-foreground"
      >
        {t("learning.audioPlayer.openExternal")}
      </a>
    </div>
  );
}
