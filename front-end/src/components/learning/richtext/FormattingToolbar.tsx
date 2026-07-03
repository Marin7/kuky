import { useTranslation } from "react-i18next";
import { Strikethrough } from "lucide-react";
import type { HighlightColor, TextColor } from "./types";
import { HIGHLIGHT_COLORS, TEXT_COLORS } from "./types";

const TEXT_COLOR_CLASS: Record<TextColor, string> = {
  red: "text-red-600",
  green: "text-green-600",
  blue: "text-blue-600",
  neutral: "text-foreground",
};

const HIGHLIGHT_COLOR_CLASS: Record<HighlightColor, string> = {
  yellow: "bg-yellow-200",
  green: "bg-green-200",
  pink: "bg-pink-200",
};

interface Props {
  disabled: boolean;
  onApplyColor: (color: TextColor | undefined) => void;
  onApplyHighlight: (highlight: HighlightColor | undefined) => void;
  onToggleStrike: () => void;
}

/**
 * A selection-based formatting bar: buttons apply to whatever text is
 * currently selected in the paired RichTextEditor. `onMouseDown` calls
 * preventDefault() on every button so clicking a button never steals focus
 * from the textarea and collapses the selection before the click fires.
 */
export function FormattingToolbar({
  disabled,
  onApplyColor,
  onApplyHighlight,
  onToggleStrike,
}: Props) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-wrap items-center gap-3 rounded-md border bg-muted/30 p-2">
      <div className="flex items-center gap-1">
        <span className="mr-1 text-xs text-muted-foreground">
          {t("richText.color")}
        </span>
        {TEXT_COLORS.map((color) => (
          <button
            key={color}
            type="button"
            disabled={disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => onApplyColor(color)}
            title={t(`richText.colors.${color}` as never)}
            aria-label={t(`richText.colors.${color}` as never)}
            className={`h-6 w-6 rounded-full border-2 border-transparent text-sm font-semibold hover:border-foreground/40 disabled:opacity-40 ${TEXT_COLOR_CLASS[color]}`}
          >
            A
          </button>
        ))}
        <button
          type="button"
          disabled={disabled}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => onApplyColor(undefined)}
          title={t("richText.clearColor")}
          aria-label={t("richText.clearColor")}
          className="h-6 w-6 rounded-full border text-xs text-muted-foreground hover:border-foreground/40 disabled:opacity-40"
        >
          ×
        </button>
      </div>

      <div className="flex items-center gap-1">
        <span className="mr-1 text-xs text-muted-foreground">
          {t("richText.highlight")}
        </span>
        {HIGHLIGHT_COLORS.map((highlight) => (
          <button
            key={highlight}
            type="button"
            disabled={disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => onApplyHighlight(highlight)}
            title={t(`richText.highlights.${highlight}` as never)}
            aria-label={t(`richText.highlights.${highlight}` as never)}
            className={`h-6 w-6 rounded border-2 border-transparent hover:border-foreground/40 disabled:opacity-40 ${HIGHLIGHT_COLOR_CLASS[highlight]}`}
          />
        ))}
        <button
          type="button"
          disabled={disabled}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => onApplyHighlight(undefined)}
          title={t("richText.clearHighlight")}
          aria-label={t("richText.clearHighlight")}
          className="h-6 w-6 rounded border text-xs text-muted-foreground hover:border-foreground/40 disabled:opacity-40"
        >
          ×
        </button>
      </div>

      <button
        type="button"
        disabled={disabled}
        onMouseDown={(e) => e.preventDefault()}
        onClick={() => onToggleStrike()}
        title={t("richText.strike")}
        aria-label={t("richText.strike")}
        className="flex h-6 w-6 items-center justify-center rounded border hover:border-foreground/40 disabled:opacity-40"
      >
        <Strikethrough className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}
