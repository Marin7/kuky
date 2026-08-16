import { useRef, useState } from "react";
import {
  applyFormat,
  mapTextareaOffset,
  MAX_VISIBLE_LENGTH,
  normalizeFormattedNewlines,
  normalizeNewlines,
  plainText,
  reconcileEdit,
  styleAtCaret,
  toggleStrike,
  visibleLength,
  type FormattedText,
  type HighlightColor,
  type SegmentStyle,
  type TextColor,
} from "./types";
import { FormattingToolbar } from "./FormattingToolbar";
import { RichTextViewer } from "./RichTextViewer";
import { insertAtCaret } from "./classroomEmojis";

/** Shared by the overlay and the textarea so wrap, padding, and font match. */
const EDITOR_TEXT_CLASS =
  "box-border px-3 py-2 font-sans text-base leading-relaxed whitespace-pre-wrap break-words break-all [overflow-wrap:anywhere] [tab-size:8] [scrollbar-gutter:stable]";

interface Props {
  value: FormattedText;
  onChange: (value: FormattedText) => void;
  disabled?: boolean;
  /** Toolbar formatting only — blocks typing/paste/delete that would change characters. */
  formatOnly?: boolean;
  placeholder?: string;
  id?: string;
  rows?: number;
  /** Classroom emoji control on the formatting bar. Ignored when formatOnly. */
  allowEmojiInsert?: boolean;
}

const NAV_KEYS = new Set([
  "ArrowLeft",
  "ArrowRight",
  "ArrowUp",
  "ArrowDown",
  "Home",
  "End",
  "PageUp",
  "PageDown",
]);

/**
 * A selection-based rich-text editor: a plain `<textarea>` (native selection
 * APIs, no contentEditable) drives typing and selection, while a styled
 * RichTextViewer mirror behind it shows color/highlight/strike live.
 *
 * Toolbar clicks apply to the current selection when one exists; with only a
 * caret they set sticky typing style so the next characters inherit it.
 */
export function RichTextEditor({
  value,
  onChange,
  disabled,
  formatOnly = false,
  placeholder,
  id,
  rows = 14,
  allowEmojiInsert = false,
}: Props) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const mirrorRef = useRef<HTMLDivElement>(null);
  // Remembered across toolbar pointer-downs / brief blur so format still
  // applies when the native selection momentarily collapses.
  const selectionRef = useRef({ start: 0, end: 0 });
  const [pending, setPendingState] = useState<SegmentStyle>({});
  const pendingRef = useRef(pending);
  const setPending = (
    update: SegmentStyle | ((prev: SegmentStyle) => SegmentStyle),
  ) => {
    setPendingState((prev) => {
      const next = typeof update === "function" ? update(prev) : update;
      pendingRef.current = next;
      return next;
    });
  };
  const storedText = plainText(value);
  const text = normalizeNewlines(storedText);
  const showPlaceholder = text.length === 0 && !!placeholder;

  const toStoredOffset = (apiOffset: number) =>
    mapTextareaOffset(storedText, apiOffset);

  const rememberSelection = () => {
    const el = textareaRef.current;
    if (!el) return;
    selectionRef.current = {
      start: el.selectionStart ?? 0,
      end: el.selectionEnd ?? 0,
    };
  };

  const syncPendingFromCaret = () => {
    const { start, end } = selectionRef.current;
    if (start !== end) return;
    setPending(styleAtCaret(value, toStoredOffset(start)));
  };

  const syncMirrorScroll = () => {
    const el = textareaRef.current;
    const mirror = mirrorRef.current;
    if (!el || !mirror) return;
    mirror.scrollTop = el.scrollTop;
    mirror.scrollLeft = el.scrollLeft;
  };

  const focusEditor = () => {
    requestAnimationFrame(() => {
      textareaRef.current?.focus();
    });
  };

  const restoreSelection = (start: number, end: number) => {
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

  const currentRange = () => {
    const el = textareaRef.current;
    const liveStart = el?.selectionStart;
    const liveEnd = el?.selectionEnd;
    if (liveStart != null && liveEnd != null && liveStart !== liveEnd) {
      return { start: liveStart, end: liveEnd };
    }
    const remembered = selectionRef.current;
    if (remembered.start !== remembered.end) {
      return remembered;
    }
    const caret = liveStart ?? remembered.start;
    return { start: caret, end: liveEnd ?? remembered.end ?? caret };
  };

  const handleTextChange = (newText: string) => {
    if (formatOnly) return;
    const nextText = normalizeNewlines(newText);
    const current =
      storedText === text ? value : normalizeFormattedNewlines(value);
    onChange(
      reconcileEdit(current, plainText(current), nextText, pendingRef.current),
    );
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLTextAreaElement>) => {
    e.preventDefault();
    if (formatOnly) return;
    // Only ever read the plain-text MIME type — this is what strips any
    // foreign styling, links, or images carried by content copied from
    // another application, regardless of source.
    const el = textareaRef.current;
    if (!el) return;
    const pasted = normalizeNewlines(e.clipboardData.getData("text/plain"));
    const { selectionStart, selectionEnd } = el;
    const before = text.slice(0, selectionStart);
    const after = text.slice(selectionEnd);
    const available = MAX_VISIBLE_LENGTH - before.length - after.length;
    const clipped = pasted.slice(0, Math.max(0, available));
    const newText = before + clipped + after;
    const current =
      storedText === text ? value : normalizeFormattedNewlines(value);
    onChange(
      reconcileEdit(current, plainText(current), newText, pendingRef.current),
    );
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

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (!formatOnly) return;
    // Allow navigation / shortcuts that don't mutate text; block typing & delete.
    if (e.ctrlKey || e.metaKey || e.altKey) return;
    if (NAV_KEYS.has(e.key) || e.key === "Tab" || e.key === "Escape") return;
    e.preventDefault();
  };

  const handleApplyColor = (color: TextColor | undefined) => {
    const { start, end } = currentRange();
    if (start !== end) {
      onChange(
        applyFormat(value, toStoredOffset(start), toStoredOffset(end), {
          color,
        }),
      );
      restoreSelection(start, end);
    } else {
      focusEditor();
    }
    setPending((prev) => {
      const next = { ...prev };
      if (color === undefined) delete next.color;
      else next.color = color;
      return next;
    });
  };

  const handleApplyHighlight = (highlight: HighlightColor | undefined) => {
    const { start, end } = currentRange();
    if (start !== end) {
      onChange(
        applyFormat(value, toStoredOffset(start), toStoredOffset(end), {
          highlight,
        }),
      );
      restoreSelection(start, end);
    } else {
      focusEditor();
    }
    setPending((prev) => {
      const next = { ...prev };
      if (highlight === undefined) delete next.highlight;
      else next.highlight = highlight;
      return next;
    });
  };

  const handleToggleStrike = () => {
    const { start, end } = currentRange();
    if (start !== end) {
      const storedStart = toStoredOffset(start);
      const next = toggleStrike(value, storedStart, toStoredOffset(end));
      onChange(next);
      const after = styleAtCaret(next, storedStart + 1);
      setPending((prev) => {
        const sticky = { ...prev };
        if (after.strike) sticky.strike = true;
        else delete sticky.strike;
        return sticky;
      });
      restoreSelection(start, end);
      return;
    }
    setPending((prev) => {
      const sticky = { ...prev };
      if (sticky.strike) delete sticky.strike;
      else sticky.strike = true;
      return sticky;
    });
    focusEditor();
  };

  const handleInsertEmoji = (emoji: string) => {
    if (formatOnly) return;
    const { start, end } = currentRange();
    const result = insertAtCaret(text, start, end, emoji, MAX_VISIBLE_LENGTH);
    if (result.text === text) {
      restoreSelection(start, end);
      return;
    }
    const current =
      storedText === text ? value : normalizeFormattedNewlines(value);
    onChange(
      reconcileEdit(
        current,
        plainText(current),
        result.text,
        pendingRef.current,
      ),
    );
    restoreSelection(result.caret, result.caret);
  };

  // Overlay text must match the textarea's LF-normalized value so wrap
  // and selection stay aligned; formatting still uses stored offsets.
  const mirrorValue =
    storedText === text
      ? value
      : value.map((seg) => ({ ...seg, text: normalizeNewlines(seg.text) }));
  // Trailing newline alone collapses in a pre-wrap mirror; keep height in sync
  // with the textarea caret row.
  const mirrorSegments: FormattedText = text.endsWith("\n")
    ? [...mirrorValue, { text: "\u200b" }]
    : mirrorValue;

  return (
    <div className="space-y-2">
      <FormattingToolbar
        disabled={!!disabled}
        activeColor={pending.color}
        activeHighlight={pending.highlight}
        activeStrike={!!pending.strike}
        onApplyColor={handleApplyColor}
        onApplyHighlight={handleApplyHighlight}
        onToggleStrike={handleToggleStrike}
        allowEmojiInsert={allowEmojiInsert && !formatOnly}
        onInsertEmoji={handleInsertEmoji}
      />

      <div
        className={`relative rounded-md border border-input shadow-sm focus-within:ring-1 focus-within:ring-ring ${disabled ? "opacity-50" : ""}`}
      >
        <div
          ref={mirrorRef}
          aria-hidden
          className={`pointer-events-none absolute inset-0 overflow-hidden ${EDITOR_TEXT_CLASS}`}
        >
          {showPlaceholder ? (
            <span className="text-muted-foreground">{placeholder}</span>
          ) : (
            <RichTextViewer segments={mirrorSegments} className="contents" />
          )}
        </div>

        <textarea
          ref={textareaRef}
          id={id}
          value={text}
          onChange={(e) => handleTextChange(e.target.value)}
          onPaste={handlePaste}
          onKeyDown={handleKeyDown}
          onSelect={rememberSelection}
          onKeyUp={(e) => {
            rememberSelection();
            if (NAV_KEYS.has(e.key)) syncPendingFromCaret();
          }}
          onClick={() => {
            rememberSelection();
            syncPendingFromCaret();
          }}
          onScroll={syncMirrorScroll}
          placeholder={placeholder}
          rows={rows}
          maxLength={MAX_VISIBLE_LENGTH}
          disabled={disabled}
          readOnly={formatOnly}
          style={{ WebkitTextFillColor: "transparent" }}
          className={`relative z-10 block min-h-[14rem] w-full resize-none appearance-none overflow-auto border-0 bg-transparent text-transparent caret-foreground placeholder:text-transparent focus-visible:outline-none disabled:cursor-not-allowed ${EDITOR_TEXT_CLASS}`}
        />
      </div>

      {!formatOnly && (
        <p className="text-right text-xs text-muted-foreground tabular-nums">
          {visibleLength(value)} / {MAX_VISIBLE_LENGTH}
        </p>
      )}
    </div>
  );
}
