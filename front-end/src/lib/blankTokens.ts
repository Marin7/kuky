// Shared "___" blank-token parsing for MULTI_BLANK / DRAG_DROP passages.
// Mirrors the back-end's exact-three-underscore rule (BlankPassageParser.java):
// a blank is exactly three underscores not adjacent to another underscore.
const BLANK_SOURCE = "(?<!_)___(?!_)";

/** Number of blank tokens in `prompt`. */
export function countBlanks(prompt: string): number {
  if (!prompt) return 0;
  const matches = prompt.match(new RegExp(BLANK_SOURCE, "g"));
  return matches ? matches.length : 0;
}

export type PromptSegment =
  | { type: "text"; text: string }
  | { type: "blank"; index: number };

/**
 * Splits a passage into alternating text segments and blank markers
 * (left-to-right blank order, matching `BlankPassageParser.splitSegments`).
 */
export function splitPromptSegments(prompt: string): PromptSegment[] {
  const segments: PromptSegment[] = [];
  if (!prompt) return [{ type: "text", text: "" }];
  const regex = new RegExp(BLANK_SOURCE, "g");
  let lastIndex = 0;
  let blankIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(prompt)) !== null) {
    segments.push({ type: "text", text: prompt.slice(lastIndex, match.index) });
    segments.push({ type: "blank", index: blankIndex });
    blankIndex += 1;
    lastIndex = match.index + match[0].length;
  }
  segments.push({ type: "text", text: prompt.slice(lastIndex) });
  return segments;
}
