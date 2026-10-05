import type { SpotWrongWordsError } from "@/lib/admin";

/**
 * SPOT_WRONG_WORDS word segmentation (specs/054-spot-wrong-words).
 * One word = letters/marks/digits, optionally joined by an internal apostrophe
 * or hyphen. MUST stay identical to the regex in back-end `SpotWrongWords.java`,
 * which validates the teacher's word indices on save.
 */
const WORD = /[\p{L}\p{M}\p{N}]+(?:['’-][\p{L}\p{M}\p{N}]+)*/gu;

export interface TextSegment {
  text: string;
  /** Word index, or null for the gap (spaces, punctuation, newlines) between words. */
  wordIndex: number | null;
}

export function tokenize(text: string): string[] {
  return text.match(WORD) ?? [];
}

/** Words and the verbatim gaps between them, in order. */
export function segment(text: string): TextSegment[] {
  const segments: TextSegment[] = [];
  let last = 0;
  let wordIndex = 0;
  for (const match of text.matchAll(WORD)) {
    const start = match.index ?? 0;
    if (start > last) {
      segments.push({ text: text.slice(last, start), wordIndex: null });
    }
    segments.push({ text: match[0], wordIndex: wordIndex++ });
    last = start + match[0].length;
  }
  if (last < text.length) {
    segments.push({ text: text.slice(last), wordIndex: null });
  }
  return segments;
}

/**
 * Keeps each marked error on its word after the text is edited: an LCS over
 * the old and new word lists moves matched errors to their new index;
 * errors on deleted or changed words are dropped.
 */
export function carryMarks(
  oldText: string,
  newText: string,
  errors: SpotWrongWordsError[],
): SpotWrongWordsError[] {
  if (errors.length === 0) return errors;
  const a = tokenize(oldText);
  const b = tokenize(newText);
  const n = a.length;
  const m = b.length;
  const lcs: number[][] = Array.from({ length: n + 1 }, () =>
    new Array<number>(m + 1).fill(0),
  );
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      lcs[i][j] =
        a[i] === b[j]
          ? lcs[i + 1][j + 1] + 1
          : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
    }
  }
  const newIndexOf = new Map<number, number>();
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (a[i] === b[j]) {
      newIndexOf.set(i++, j++);
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      i++;
    } else {
      j++;
    }
  }
  return errors.flatMap((e) => {
    const to = newIndexOf.get(e.wordIndex);
    return to === undefined ? [] : [{ ...e, wordIndex: to, word: b[to] }];
  });
}
