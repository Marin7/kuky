import { useRef } from "react";
import { useTranslation } from "react-i18next";
import {
  applyFormat,
  MAX_VISIBLE_LENGTH,
  plainText,
  reconcileEdit,
  toggleStrike,
  visibleLength,
  type FormattedText,
  type HighlightColor,
  type TextColor,
} from "./types";
import { FormattingToolbar } from "./FormattingToolbar";
import { RichTextViewer } from "./RichTextViewer";

interface Props {
  value: FormattedText;
  onChange: (value: FormattedText) => void;
  disabled?: boolean;
  placeholder?: string;
  id?: string;
  rows?: number;
}

/**
 * A selection-based rich-text editor: a plain `<textarea>` (native selection
 * APIs, no contentEditable) drives the visible text, while the
 * FormattingToolbar applies color/highlight/strike to whatever range is
 * currently selected. Because a textarea can't render per-character styling
 * itself, a read-only RichTextViewer beneath it mirrors the current value so
 * applied formatting is immediately visible.
 */
export function RichTextEditor({
  value,
  onChange,
  disabled,
  placeholder,
  id,
  rows = 14,
}: Props) {
  const { t } = useTranslation();
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const text = plainText(value);

  const currentSelection = () => {
    const el = textareaRef.current;
    if (!el) return { start: 0, end: 0 };
    return { start: el.selectionStart ?? 0, end: el.selectionEnd ?? 0 };
  };

  const handleTextChange = (newText: string) => {
    onChange(reconcileEdit(value, text, newText));
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLTextAreaElement>) => {
    // Only ever read the plain-text MIME type — this is what strips any
    // foreign styling, links, or images carried by content copied from
    // another application, regardless of source.
    e.preventDefault();
    const el = textareaRef.current;
    if (!el) return;
    const pasted = e.clipboardData.getData("text/plain");
    const { selectionStart, selectionEnd } = el;
    const before = text.slice(0, selectionStart);
    const after = text.slice(selectionEnd);
    const available = MAX_VISIBLE_LENGTH - before.length - after.length;
    const clipped = pasted.slice(0, Math.max(0, available));
    const newText = before + clipped + after;
    onChange(reconcileEdit(value, text, newText));
    requestAnimationFrame(() => {
      const pos = before.length + clipped.length;
      const current = textareaRef.current;
      if (current) {
        current.selectionStart = pos;
        current.selectionEnd = pos;
      }
    });
  };

  const withSelection = (fn: (start: number, end: number) => FormattedText) => {
    const { start, end } = currentSelection();
    if (start === end) return;
    onChange(fn(start, end));
  };

  const handleApplyColor = (color: TextColor | undefined) =>
    withSelection((start, end) => applyFormat(value, start, end, { color }));
  const handleApplyHighlight = (highlight: HighlightColor | undefined) =>
    withSelection((start, end) =>
      applyFormat(value, start, end, { highlight }),
    );
  const handleToggleStrike = () =>
    withSelection((start, end) => toggleStrike(value, start, end));

  return (
    <div className="space-y-2">
      <FormattingToolbar
        disabled={!!disabled}
        onApplyColor={handleApplyColor}
        onApplyHighlight={handleApplyHighlight}
        onToggleStrike={handleToggleStrike}
      />

      <textarea
        ref={textareaRef}
        id={id}
        value={text}
        onChange={(e) => handleTextChange(e.target.value)}
        onPaste={handlePaste}
        placeholder={placeholder}
        rows={rows}
        maxLength={MAX_VISIBLE_LENGTH}
        disabled={disabled}
        className="min-h-[14rem] w-full resize-none overflow-hidden rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
      />

      <div className="flex items-baseline justify-between gap-3">
        <span className="text-xs font-medium text-muted-foreground">
          {t("richText.preview")}
        </span>
        <span className="text-xs text-muted-foreground tabular-nums">
          {visibleLength(value)} / {MAX_VISIBLE_LENGTH}
        </span>
      </div>
      <div className="rounded-md border bg-muted/20 p-3">
        <RichTextViewer segments={value} />
      </div>
    </div>
  );
}
