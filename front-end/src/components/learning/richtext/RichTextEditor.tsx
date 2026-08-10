import { useRef } from "react";
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
 * APIs, no contentEditable) drives typing and selection, while a styled
 * RichTextViewer mirror behind it shows color/highlight/strike live. The
 * FormattingToolbar applies formatting to the current (or last remembered)
 * selection without stealing focus.
 */
export function RichTextEditor({
  value,
  onChange,
  disabled,
  placeholder,
  id,
  rows = 14,
}: Props) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const mirrorRef = useRef<HTMLDivElement>(null);
  // Remembered across toolbar pointer-downs / brief blur so format still
  // applies when the native selection momentarily collapses.
  const selectionRef = useRef({ start: 0, end: 0 });
  const text = plainText(value);
  const showPlaceholder = text.length === 0 && !!placeholder;

  const rememberSelection = () => {
    const el = textareaRef.current;
    if (!el) return;
    selectionRef.current = {
      start: el.selectionStart ?? 0,
      end: el.selectionEnd ?? 0,
    };
  };

  const syncMirrorScroll = () => {
    const el = textareaRef.current;
    const mirror = mirrorRef.current;
    if (!el || !mirror) return;
    mirror.scrollTop = el.scrollTop;
    mirror.scrollLeft = el.scrollLeft;
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
        selectionRef.current = { start: pos, end: pos };
      }
    });
  };

  const withSelection = (fn: (start: number, end: number) => FormattedText) => {
    const el = textareaRef.current;
    const liveStart = el?.selectionStart;
    const liveEnd = el?.selectionEnd;
    const start =
      liveStart != null && liveEnd != null && liveStart !== liveEnd
        ? liveStart
        : selectionRef.current.start;
    const end =
      liveStart != null && liveEnd != null && liveStart !== liveEnd
        ? liveEnd
        : selectionRef.current.end;
    if (start === end) return;
    onChange(fn(start, end));
    requestAnimationFrame(() => {
      const current = textareaRef.current;
      if (!current) return;
      current.focus();
      current.selectionStart = start;
      current.selectionEnd = end;
      selectionRef.current = { start, end };
      syncMirrorScroll();
    });
  };

  const handleApplyColor = (color: TextColor | undefined) =>
    withSelection((start, end) => applyFormat(value, start, end, { color }));
  const handleApplyHighlight = (highlight: HighlightColor | undefined) =>
    withSelection((start, end) =>
      applyFormat(value, start, end, { highlight }),
    );
  const handleToggleStrike = () =>
    withSelection((start, end) => toggleStrike(value, start, end));

  // Trailing newline alone collapses in a pre-wrap mirror; keep height in sync
  // with the textarea caret row.
  const mirrorSegments: FormattedText =
    text.endsWith("\n") ? [...value, { text: "\u200b" }] : value;

  return (
    <div className="space-y-2">
      <FormattingToolbar
        disabled={!!disabled}
        onApplyColor={handleApplyColor}
        onApplyHighlight={handleApplyHighlight}
        onToggleStrike={handleToggleStrike}
      />

      <div className={`relative ${disabled ? "opacity-50" : ""}`}>
        <div
          ref={mirrorRef}
          aria-hidden
          className="pointer-events-none absolute inset-0 overflow-auto rounded-md px-3 py-2 text-base leading-relaxed"
        >
          {showPlaceholder ? (
            <span className="text-muted-foreground">{placeholder}</span>
          ) : (
            <RichTextViewer segments={mirrorSegments} />
          )}
        </div>

        <textarea
          ref={textareaRef}
          id={id}
          value={text}
          onChange={(e) => handleTextChange(e.target.value)}
          onPaste={handlePaste}
          onSelect={rememberSelection}
          onKeyUp={rememberSelection}
          onClick={rememberSelection}
          onScroll={syncMirrorScroll}
          placeholder={placeholder}
          rows={rows}
          maxLength={MAX_VISIBLE_LENGTH}
          disabled={disabled}
          style={{ WebkitTextFillColor: "transparent" }}
          className="relative z-10 min-h-[14rem] w-full resize-none overflow-auto rounded-md border border-input bg-transparent px-3 py-2 text-base leading-relaxed text-transparent caret-foreground shadow-sm placeholder:text-transparent focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed"
        />
      </div>

      <p className="text-right text-xs text-muted-foreground tabular-nums">
        {visibleLength(value)} / {MAX_VISIBLE_LENGTH}
      </p>
    </div>
  );
}
