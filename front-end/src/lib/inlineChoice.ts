export interface InlineChoiceOption {
  id: string;
  label: string;
}

export interface InlineChoiceToken {
  /** Option text as it appears in the prompt (not the stored label). */
  text: string;
  optionId: string;
}

export interface InlineChoiceMatch {
  /** Index of the opening `(` in the prompt. */
  start: number;
  /** Index after the closing `)`. */
  end: number;
  tokens: InlineChoiceToken[];
}

function normalizeLabel(value: string): string {
  return value.trim().normalize("NFC").toLocaleLowerCase("es");
}

/**
 * Classic opción única: one `(a / b)` (or more slash-separated options)
 * whose tokens uniquely match every option label (case-insensitive).
 * Numbered `(1)`…`(N)` items are excluded by the caller.
 *
 * Returns null when there is no unique matching group — radios stay.
 */
export function matchInlineChoiceGroup(
  prompt: string,
  options: InlineChoiceOption[],
): InlineChoiceMatch | null {
  if (!prompt || options.length < 2) return null;

  const optionByNorm = new Map<string, string>();
  for (const option of options) {
    const key = normalizeLabel(option.label);
    if (!key || optionByNorm.has(key)) return null;
    optionByNorm.set(key, option.id);
  }

  const groupRe = /\(([^()]+)\)/g;
  const matches: InlineChoiceMatch[] = [];
  let found: RegExpExecArray | null;
  while ((found = groupRe.exec(prompt)) !== null) {
    const inner = found[1];
    if (!inner.includes("/")) continue;
    const parts = inner
      .split("/")
      .map((part) => part.trim())
      .filter((part) => part.length > 0);
    if (parts.length !== options.length) continue;

    const tokens: InlineChoiceToken[] = [];
    const usedIds = new Set<string>();
    let ok = true;
    for (const text of parts) {
      const optionId = optionByNorm.get(normalizeLabel(text));
      if (!optionId || usedIds.has(optionId)) {
        ok = false;
        break;
      }
      usedIds.add(optionId);
      tokens.push({ text, optionId });
    }
    if (!ok || usedIds.size !== options.length) continue;
    matches.push({
      start: found.index,
      end: found.index + found[0].length,
      tokens,
    });
  }

  return matches.length === 1 ? matches[0] : null;
}

export function matchClassicInlineSingleChoice(question: {
  kind: string;
  prompt: string;
  options: InlineChoiceOption[];
  structure?: { items?: unknown[] };
}): InlineChoiceMatch | null {
  if (question.kind !== "SINGLE_CHOICE") return null;
  if ((question.structure?.items?.length ?? 0) > 0) return null;
  return matchInlineChoiceGroup(question.prompt, question.options);
}

const ELLIPSIS_RE = /\.{3}|…/g;

function isClassicSingleChoice(question: {
  kind: string;
  structure?: { items?: unknown[] };
}): boolean {
  return (
    question.kind === "SINGLE_CHOICE" &&
    (question.structure?.items?.length ?? 0) === 0
  );
}

/** Exactly one `...` / `…` in a classic opción única prompt. */
export function hasSingleEllipsisChoice(question: {
  kind: string;
  prompt: string;
  structure?: { items?: unknown[] };
}): boolean {
  if (!isClassicSingleChoice(question)) return false;
  const matches = question.prompt?.match(ELLIPSIS_RE);
  return matches?.length === 1;
}

/** Rewrites the single ellipsis as a `___` blank so MultiBlankResult can fill it. */
export function ellipsisPromptAsBlank(prompt: string): string {
  return prompt.replace(/\.{3}|…/, "___");
}

/**
 * Result-view passage for classic radios: fill a single `...`, otherwise
 * append a blank so the chosen label still shows (e.g. "Cincuenta y seis es:").
 * Numbered items and `(a / b)` inline choice are left to their own renderers.
 */
export function classicSingleChoiceResultPrompt(question: {
  kind: string;
  prompt: string;
  options: InlineChoiceOption[];
  structure?: { items?: unknown[] };
}): string | null {
  if (!isClassicSingleChoice(question)) return null;
  if (matchClassicInlineSingleChoice(question)) return null;
  if (hasSingleEllipsisChoice(question)) {
    return ellipsisPromptAsBlank(question.prompt);
  }
  return `${question.prompt.replace(/\s+$/, "")} ___`;
}
