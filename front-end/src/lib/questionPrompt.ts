/** Leading worksheet-style `N.` at the start of a prompt (e.g. seed / PDF copy). */
const LEADING_ENUM_RE = /^\d+\.\s*/;

/** Drop a teacher-authored leading `N.` so the UI index is not doubled. */
export function stripLeadingEnumeration(prompt: string): string {
  return prompt.replace(LEADING_ENUM_RE, "");
}

/** True when the prompt already starts with its own `N.` numbering. */
export function promptHasLeadingEnumeration(prompt: string): boolean {
  return LEADING_ENUM_RE.test(prompt);
}

/** Take/result label: UI index + prompt without a duplicate leading `N.`. */
export function numberedPromptLabel(number: number, prompt: string): string {
  return `${number}. ${stripLeadingEnumeration(prompt)}`;
}
