import type { FormattedText, HighlightColor, TextColor } from "./types";

const TEXT_COLOR_CLASS: Record<TextColor, string> = {
  red: "text-red-600",
  green: "text-green-600",
  blue: "text-blue-600",
  neutral: "",
};

const HIGHLIGHT_COLOR_CLASS: Record<HighlightColor, string> = {
  yellow: "bg-yellow-200",
  green: "bg-green-200",
  pink: "bg-pink-200",
};

interface Props {
  segments: FormattedText;
  className?: string;
}

/**
 * Read-only render of a FormattedText value. Every segment's `text` is
 * rendered as a normal React text child (never dangerouslySetInnerHTML), so
 * markup-like content typed or pasted into a segment is always displayed as
 * inert plain text — this is what guarantees no stored content can execute as
 * markup, independent of anything the editor does on the way in.
 */
export function RichTextViewer({ segments, className }: Props) {
  return (
    <div
      className={`whitespace-pre-wrap break-words text-sm ${className ?? ""}`}
    >
      {segments.map((segment, index) => (
        <span
          key={index}
          className={[
            segment.color ? TEXT_COLOR_CLASS[segment.color] : "",
            segment.highlight ? HIGHLIGHT_COLOR_CLASS[segment.highlight] : "",
            segment.strike ? "line-through" : "",
          ]
            .filter(Boolean)
            .join(" ")}
        >
          {segment.text}
        </span>
      ))}
    </div>
  );
}
