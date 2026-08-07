import { API_ORIGIN } from "@/lib/api";

/** Extract an 11-char YouTube video id from common URL shapes (or a bare id). */
export function extractYoutubeVideoId(raw: string | null | undefined): string | null {
  if (!raw?.trim()) return null;
  const url = raw.trim();
  if (/^[A-Za-z0-9_-]{11}$/.test(url)) return url;

  const patterns = [
    /(?:youtube\.com|youtube-nocookie\.com)\/watch\?(?:[^#]*&)?v=([A-Za-z0-9_-]{11})/,
    /(?:youtube\.com|youtube-nocookie\.com)\/shorts\/([A-Za-z0-9_-]{11})/,
    /(?:youtube\.com|youtube-nocookie\.com)\/embed\/([A-Za-z0-9_-]{11})/,
    /youtu\.be\/([A-Za-z0-9_-]{11})/,
  ];
  for (const p of patterns) {
    const m = url.match(p);
    if (m?.[1]) return m[1];
  }
  return null;
}

export function youtubeEmbedUrl(videoId: string): string {
  return `https://www.youtube-nocookie.com/embed/${videoId}`;
}

export function activityImageUrl(imageId: string): string {
  return `${API_ORIGIN}/api/v1/images/${imageId}`;
}
