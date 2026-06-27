import { useTranslation } from "react-i18next";
import { audioFileUrl } from "@/lib/learning";

interface Props {
  audioUrl: string | null;
  audioFileId: string | null;
}

/** Extracts a YouTube video id from the common watch / short / embed URL forms. */
function youTubeId(url: string): string | null {
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
 * Renders the audio source of a listening homework. Uploaded files and direct
 * audio links play in a native <audio> element; YouTube/Vimeo links embed as an
 * iframe. Returns null when there is no source.
 */
export function AudioPlayer({ audioUrl, audioFileId }: Props) {
  const { t } = useTranslation();

  if (audioFileId) {
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

  // Assume a direct audio link; offer a fallback link to open it elsewhere.
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
