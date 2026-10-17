import { useTranslation } from "react-i18next";
import { MAX_CONTINUATION_CHARS } from "@/lib/sentenceStarter";
import { Textarea } from "@/components/ui/textarea";

interface Props {
  starters: string[];
  values: string[];
  onChange: (index: number, value: string) => void;
  disabled?: boolean;
}

/**
 * SENTENCE_STARTER answer: each starter is fixed text the student cannot
 * edit, followed by an input for the rest of the sentence (specs/057).
 */
export function SentenceStarterInput({
  starters,
  values,
  onChange,
  disabled = false,
}: Props) {
  const { t } = useTranslation();
  return (
    <ol className="space-y-3">
      {starters.map((starter, i) => {
        const value = values[i] ?? "";
        return (
          <li
            key={i}
            className="flex min-w-0 flex-col gap-1 sm:flex-row sm:items-start sm:gap-2"
          >
            <span className="shrink-0 font-medium text-foreground sm:pt-2 [overflow-wrap:anywhere]">
              {starter}
            </span>
            <div className="min-w-0 flex-1 space-y-1">
              <Textarea
                value={value}
                rows={1}
                maxLength={MAX_CONTINUATION_CHARS}
                disabled={disabled}
                onChange={(e) => onChange(i, e.target.value)}
                placeholder={t("learning.sentenceStarter.placeholder")}
                aria-label={starter}
                className="min-h-9 resize-y"
              />
              {value.length >= MAX_CONTINUATION_CHARS - 50 && (
                <p className="text-right text-xs text-muted-foreground">
                  {t("learning.sentenceStarter.counter", {
                    count: value.length,
                    max: MAX_CONTINUATION_CHARS,
                  })}
                </p>
              )}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
