/** Fixed classroom emoji set and UTF-16-safe insert (spec 046). */

export const CLASSROOM_EMOJIS = [
  "😀",
  "😊",
  "😂",
  "😍",
  "🤔",
  "😅",
  "😎",
  "😢",
  "😮",
  "👍",
  "👎",
  "👏",
  "🙏",
  "👋",
  "❤️",
  "💕",
  "⭐",
  "✅",
  "❌",
  "💡",
  "📚",
  "✏️",
  "🎉",
  "💪",
  "🔥",
  "💯",
] as const;

export type ClassroomEmoji = (typeof CLASSROOM_EMOJIS)[number];

export interface InsertAtCaretResult {
  text: string;
  caret: number;
}

/**
 * Inserts `emoji` at [start, end), or replaces that range. If the result would
 * exceed `maxLength` (UTF-16 units), returns the original text unchanged.
 */
export function insertAtCaret(
  text: string,
  start: number,
  end: number,
  emoji: string,
  maxLength: number,
): InsertAtCaretResult {
  const from = Math.max(0, Math.min(start, end, text.length));
  const to = Math.max(0, Math.min(Math.max(start, end), text.length));
  const before = text.slice(0, from);
  const after = text.slice(to);
  if (before.length + emoji.length + after.length > maxLength) {
    return { text, caret: to };
  }
  return { text: before + emoji + after, caret: from + emoji.length };
}

export function insertEmojiIntoTextarea(
  el: HTMLTextAreaElement | null,
  text: string,
  emoji: string,
  maxLength: number,
): InsertAtCaretResult {
  const start = el?.selectionStart ?? text.length;
  const end = el?.selectionEnd ?? text.length;
  return insertAtCaret(text, start, end, emoji, maxLength);
}
