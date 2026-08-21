/** Render-time URL detection for authored / submitted plain text. */

type LinkifiedPart =
  | { type: "text"; text: string }
  | { type: "link"; text: string; href: string };

const URL_RE = /\b(?:https?:\/\/|www\.)[^\s<>"'`\\]+/gi;

const TRAILING_PUNCT = new Set([
  ".",
  ",",
  ";",
  ":",
  "!",
  "?",
  "'",
  '"',
  "”",
  "’",
]);

function countChar(s: string, ch: string): number {
  let n = 0;
  for (const c of s) if (c === ch) n++;
  return n;
}

/** Drop wrapping punctuation that is not part of the URL. */
function trimUrlMatch(raw: string): { url: string; trailing: string } {
  let end = raw.length;
  while (end > 0) {
    const ch = raw[end - 1]!;
    if (ch === ")" || ch === "]" || ch === "}") {
      const open = ch === ")" ? "(" : ch === "]" ? "[" : "{";
      const prefix = raw.slice(0, end);
      if (countChar(prefix, ch) > countChar(prefix, open)) {
        end--;
        continue;
      }
      break;
    }
    if (TRAILING_PUNCT.has(ch)) {
      end--;
      continue;
    }
    break;
  }
  return { url: raw.slice(0, end), trailing: raw.slice(end) };
}

function toSafeHref(url: string): string | null {
  if (!url || url.length > 2048) return null;
  const candidate = /^www\./i.test(url) ? `https://${url}` : url;
  if (!/^https?:\/\//i.test(candidate)) return null;
  try {
    const parsed = new URL(candidate);
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
      return null;
    }
    if (parsed.username || parsed.password) return null;
    if (!parsed.hostname || !parsed.hostname.includes(".")) return null;
    return parsed.href;
  } catch {
    return null;
  }
}

/**
 * Splits `text` into plain runs and http(s)/www URLs. The stored string is
 * unchanged — callers render links from this scan instead of storing markup.
 */
export function splitLinkifiedText(text: string): LinkifiedPart[] {
  if (!text) return [];
  const parts: LinkifiedPart[] = [];
  const re = new RegExp(URL_RE.source, "gi");
  let last = 0;
  let match: RegExpExecArray | null;
  while ((match = re.exec(text)) !== null) {
    const raw = match[0];
    const start = match.index;
    if (start > last) {
      parts.push({ type: "text", text: text.slice(last, start) });
    }
    const { url, trailing } = trimUrlMatch(raw);
    const href = toSafeHref(url);
    if (href) {
      parts.push({ type: "link", text: url, href });
    } else if (url) {
      parts.push({ type: "text", text: url });
    }
    if (trailing) {
      parts.push({ type: "text", text: trailing });
    }
    last = start + raw.length;
  }
  if (last < text.length) {
    parts.push({ type: "text", text: text.slice(last) });
  }
  return parts;
}
