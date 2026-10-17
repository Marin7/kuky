import { useTranslation } from "react-i18next";
import { RichTextViewer } from "./richtext/RichTextViewer";
import type { FormattedText } from "./richtext/types";

interface Props {
  /** Stored plain answer: one composed sentence per line. */
  text?: string | null;
  /** Teacher-annotated version of the same text, when present. */
  formatted?: FormattedText | null;
  starters: string[];
  /** Shown under each sentence once the correction is visible. */
  modelAnswers?: (string | null)[] | null;
}

/** Splits segments into lines at "\n", keeping each segment's styles. */
function splitLines(segments: FormattedText): FormattedText[] {
  const lines: FormattedText[] = [[]];
  for (const seg of segments) {
    seg.text.split("\n").forEach((part, i) => {
      if (i > 0) lines.push([]);
      if (part) lines[lines.length - 1].push({ ...seg, text: part });
    });
  }
  return lines;
}

/** Splits a line's segments at a character offset. */
function splitAt(
  line: FormattedText,
  offset: number,
): [FormattedText, FormattedText] {
  const head: FormattedText = [];
  const tail: FormattedText = [];
  let pos = 0;
  for (const seg of line) {
    const end = pos + seg.text.length;
    if (end <= offset) head.push(seg);
    else if (pos >= offset) tail.push(seg);
    else {
      head.push({ ...seg, text: seg.text.slice(0, offset - pos) });
      tail.push({ ...seg, text: seg.text.slice(offset - pos) });
    }
    pos = end;
  }
  return [head, tail];
}

/**
 * SENTENCE_STARTER answer, one sentence per line with its starter emphasised
 * (teacher annotations kept), plus model answers when provided (specs/057).
 */
export function SentenceStarterAnswerView({
  text,
  formatted,
  starters,
  modelAnswers,
}: Props) {
  const { t } = useTranslation();
  const segments: FormattedText =
    formatted && formatted.length > 0 ? formatted : [{ text: text ?? "" }];
  const lines = splitLines(segments);
  return (
    <ol className="space-y-2">
      {lines.map((line, i) => {
        const plain = line.map((s) => s.text).join("");
        const starter = starters[i] ?? "";
        const [head, tail] =
          starter && plain.startsWith(starter)
            ? splitAt(line, starter.length)
            : [[], line];
        const model = modelAnswers?.[i];
        return (
          <li key={i} className="space-y-1">
            <div className="text-sm">
              {head.length > 0 && (
                <RichTextViewer
                  segments={head}
                  className="inline text-sm font-semibold"
                />
              )}
              <RichTextViewer segments={tail} className="inline text-sm" />
            </div>
            {model && (
              <p className="text-xs text-muted-foreground [overflow-wrap:anywhere]">
                <span className="font-medium">
                  {t("learning.sentenceStarter.modelAnswer")}:
                </span>{" "}
                {model}
              </p>
            )}
          </li>
        );
      })}
    </ol>
  );
}
