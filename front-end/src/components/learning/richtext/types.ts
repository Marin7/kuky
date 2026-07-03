// Shared rich-text model for Writing homework answers and teacher feedback.
// A FormattedText value is a JSON array of plain-text segments, each carrying
// three independent, combinable style attributes (color, highlight, strike).
// There is no field capable of holding scripts, links, or images — the format
// is safe by construction, not by sanitization (mirrors the backend's
// FormattedTextSegment record).

export type TextColor = "red" | "green" | "blue" | "neutral";
export type HighlightColor = "yellow" | "green" | "pink";

export interface Segment {
  text: string;
  color?: TextColor;
  highlight?: HighlightColor;
  strike?: boolean;
}

export type FormattedText = Segment[];

export const TEXT_COLORS: TextColor[] = ["red", "green", "blue", "neutral"];
export const HIGHLIGHT_COLORS: HighlightColor[] = ["yellow", "green", "pink"];

export const MAX_VISIBLE_LENGTH = 2000;

export function visibleLength(segments: FormattedText): number {
  return segments.reduce((total, s) => total + s.text.length, 0);
}

export function plainText(segments: FormattedText): string {
  return segments.map((s) => s.text).join("");
}

/** Splits segments so that `offset` always falls on a segment boundary. */
function splitSegmentsAt(
  segments: FormattedText,
  offset: number,
): FormattedText {
  const result: Segment[] = [];
  let pos = 0;
  for (const seg of segments) {
    const segStart = pos;
    const segEnd = pos + seg.text.length;
    if (offset > segStart && offset < segEnd) {
      const cut = offset - segStart;
      result.push({ ...seg, text: seg.text.slice(0, cut) });
      result.push({ ...seg, text: seg.text.slice(cut) });
    } else {
      result.push(seg);
    }
    pos = segEnd;
  }
  return result;
}

function mergeAdjacent(segments: FormattedText): FormattedText {
  const result: Segment[] = [];
  for (const seg of segments) {
    if (!seg.text) continue;
    const prev = result[result.length - 1];
    if (
      prev &&
      prev.color === seg.color &&
      prev.highlight === seg.highlight &&
      !!prev.strike === !!seg.strike
    ) {
      prev.text += seg.text;
    } else {
      result.push({ ...seg });
    }
  }
  return result;
}

/**
 * Applies (or merges) color/highlight/strike onto the [start, end) character
 * range, preserving whatever other attributes each affected segment already
 * has — this is what makes the three attributes independently combinable.
 */
export function applyFormat(
  segments: FormattedText,
  start: number,
  end: number,
  patch: Partial<Pick<Segment, "color" | "highlight" | "strike">>,
): FormattedText {
  if (start === end) return segments;
  let result = splitSegmentsAt(segments, start);
  result = splitSegmentsAt(result, end);
  let pos = 0;
  const patched = result.map((seg) => {
    const segStart = pos;
    pos += seg.text.length;
    const segEnd = pos;
    if (segStart >= start && segEnd <= end) {
      return { ...seg, ...patch };
    }
    return seg;
  });
  return mergeAdjacent(patched);
}

/** Toggles strike off only if every character in the range is already struck. */
export function toggleStrike(
  segments: FormattedText,
  start: number,
  end: number,
): FormattedText {
  if (start === end) return segments;
  let pos = 0;
  let allStruck = true;
  for (const seg of segments) {
    const segStart = pos;
    pos += seg.text.length;
    const segEnd = pos;
    const overlapStart = Math.max(segStart, start);
    const overlapEnd = Math.min(segEnd, end);
    if (overlapEnd > overlapStart && !seg.strike) {
      allStruck = false;
      break;
    }
  }
  return applyFormat(segments, start, end, { strike: !allStruck });
}

/**
 * Reconciles a plain-text edit (typing, deleting, pasting) against the
 * existing segment array. Newly inserted text is unformatted; formatting on
 * text outside the edited span is preserved untouched.
 */
export function reconcileEdit(
  segments: FormattedText,
  oldText: string,
  newText: string,
): FormattedText {
  if (oldText === newText) return segments;

  let prefix = 0;
  const minLen = Math.min(oldText.length, newText.length);
  while (prefix < minLen && oldText[prefix] === newText[prefix]) prefix++;

  let oldSuffixStart = oldText.length;
  let newSuffixStart = newText.length;
  while (
    oldSuffixStart > prefix &&
    newSuffixStart > prefix &&
    oldText[oldSuffixStart - 1] === newText[newSuffixStart - 1]
  ) {
    oldSuffixStart--;
    newSuffixStart--;
  }

  const insertedText = newText.slice(prefix, newSuffixStart);

  let split = splitSegmentsAt(segments, prefix);
  split = splitSegmentsAt(split, oldSuffixStart);

  const before: Segment[] = [];
  const after: Segment[] = [];
  let pos = 0;
  for (const seg of split) {
    const segStart = pos;
    pos += seg.text.length;
    const segEnd = pos;
    if (segEnd <= prefix) {
      before.push(seg);
    } else if (segStart >= oldSuffixStart) {
      after.push(seg);
    }
    // Segments fully inside [prefix, oldSuffixStart) are the replaced span — dropped.
  }

  const middle: Segment[] = insertedText ? [{ text: insertedText }] : [];
  return mergeAdjacent([...before, ...middle, ...after]);
}

export function emptyFormattedText(): FormattedText {
  return [];
}
