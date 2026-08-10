import { useRef, useState } from "react";
import {
  applyFormat,
  MAX_VISIBLE_LENGTH,
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

interface Props {
  value: FormattedText;
  onChange: (value: FormattedText) => void;
  disabled?: boolean;
  placeholder?: string;
  id?: string;
  rows?: number;
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
  placeholder,
  id,
  rows = 14,
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

  const syncPendingFromCaret = () => {
    const { start, end } = selectionRef.current;
    if (start !== end) return;
    setPending(styleAtCaret(value, start));
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
    onChange(reconcileEdit(value, text, newText, pendingRef.current));
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
    onChange(reconcileEdit(value, text, newText, pendingRef.current));
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

  const handleApplyColor = (color: TextColor | undefined) => {
    const { start, end } = currentRange();
    if (start !== end) {
      onChange(applyFormat(value, start, end, { color }));
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
      onChange(applyFormat(value, start, end, { highlight }));
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
      const next = toggleStrike(value, start, end);
      onChange(next);
      const after = styleAtCaret(next, start + 1);
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

  // Trailing newline alone collapses in a pre-wrap mirror; keep height in sync
  // with the textarea caret row.
  const mirrorSegments: FormattedText = text.endsWith("\n")
    ? [...value, { text: "\u200b" }]
    : value;

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
