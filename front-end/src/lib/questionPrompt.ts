/** Leading worksheet-style `N.` at the start of a prompt (e.g. seed / PDF copy). */
const LEADING_ENUM_RE = /^\d+\.\s*/;

/** A second `N.` on a later line — keep the passage's own numbering. */
const MULTI_ENUM_RE = /\n\s*\d+\.\s/;

/** Drop a teacher-authored leading `N.` so the UI index is not doubled. */
export function stripLeadingEnumeration(prompt: string): string {
  if (MULTI_ENUM_RE.test(prompt)) return prompt;
  return prompt.replace(LEADING_ENUM_RE, "");
}

/** Prompt body for labels: no leading `N.`, no surrounding blank lines. */
export function displayPromptText(prompt: string | null | undefined): string {
  return stripLeadingEnumeration(prompt ?? "").trim();
}

/** Visible `1. ` prefix when the homework has more than one question. */
export function questionIndexLabel(
  index: number,
  questionCount: number,
): string {
  if (questionCount <= 1) return "";
  return `${index}. `;
}
