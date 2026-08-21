import { useRef } from "react";
import { cn } from "@/lib/utils";
import { TextWithLinks } from "./TextWithLinks";

const FIELD_CLASS =
  "box-border px-3 py-2 text-base md:text-sm whitespace-pre-wrap break-words [overflow-wrap:anywhere] [scrollbar-gutter:stable]";

interface Props {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  rows?: number;
  maxLength?: number;
  disabled?: boolean;
  className?: string;
}

/**
 * Plain-text textarea that paints detected URLs as blue underlined links in a
 * mirror overlay, matching the native field's wrap and caret.
 */
export function LinkAwareTextarea({
  id,
  value,
  onChange,
  rows = 4,
  maxLength,
  disabled,
  className,
}: Props) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const mirrorRef = useRef<HTMLDivElement>(null);
  const mirrorText = value.endsWith("\n") ? `${value}\u200b` : value;

  const syncScroll = () => {
    const el = textareaRef.current;
    const mirror = mirrorRef.current;
    if (!el || !mirror) return;
    mirror.scrollTop = el.scrollTop;
    mirror.scrollLeft = el.scrollLeft;
  };

  return (
    <div
      className={cn(
        "relative rounded-md border border-input shadow-sm focus-within:ring-1 focus-within:ring-ring",
        disabled && "opacity-50",
        className,
      )}
    >
      <div
        ref={mirrorRef}
        aria-hidden
        className={cn(
          "pointer-events-none absolute inset-0 overflow-hidden",
          FIELD_CLASS,
        )}
      >
        <TextWithLinks text={mirrorText} />
      </div>
      <textarea
        ref={textareaRef}
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onScroll={syncScroll}
        rows={rows}
        maxLength={maxLength}
        disabled={disabled}
        style={{ WebkitTextFillColor: "transparent" }}
        className={cn(
          "relative z-10 block min-h-[60px] w-full resize-y appearance-none overflow-auto border-0 bg-transparent text-transparent caret-foreground focus-visible:outline-none disabled:cursor-not-allowed",
          FIELD_CLASS,
        )}
      />
    </div>
  );
}
