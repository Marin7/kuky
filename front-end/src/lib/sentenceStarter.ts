/**
 * SENTENCE_STARTER helpers (specs/057-sentence-starter-answer). Mirrors
 * back-end `SentenceStarters.java` for previews only — the server normalises
 * starters on save and composes the stored sentences on submit.
 */

export const MAX_STARTERS = 10;
export const MAX_STARTER_CHARS = 80;
export const MAX_CONTINUATION_CHARS = 500;

const TRAILING_ELLIPSIS = /(\s*(\.\.\.|…))+\s*$/;
const NO_SPACE_BEFORE = ",.;:!?…";

/** "porque..." → "porque". */
export function normalizeStarter(raw: string): string {
  return raw.trim().replace(TRAILING_ELLIPSIS, "").trim();
}

/** Starter + continuation; no space when the continuation opens with punctuation. */
export function compose(starter: string, continuation: string): string {
  const c = continuation.trim().replace(/\s*\r?\n\s*/g, " ");
  if (!c) return starter;
  return starter + (NO_SPACE_BEFORE.includes(c[0]) ? "" : " ") + c;
}

/** Stored answers keep one composed sentence per line. */
export function splitAnswerLines(text: string): string[] {
  return text.split("\n");
}
