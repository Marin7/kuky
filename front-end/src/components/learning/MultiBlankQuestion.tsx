import { splitPromptSegments } from "@/lib/blankTokens";
import {
  questionIndexLabel,
  stripLeadingEnumeration,
} from "@/lib/questionPrompt";
import { Input } from "@/components/ui/input";
import { PassageText } from "./PassageText";

interface Props {
  index: number;
  questionCount: number;
  prompt: string;
  value: string[];
  onChange: (blanks: string[]) => void;
}

/** Renders a MULTI_BLANK passage with an inline input at each `___` token. */
export function MultiBlankQuestion({
  index,
  questionCount,
  prompt,
  value,
  onChange,
}: Props) {
  const segments = splitPromptSegments(stripLeadingEnumeration(prompt));

  const setBlank = (index: number, text: string) => {
    const next = [...value];
    next[index] = text;
    onChange(next);
  };

  return (
    <div className="text-base font-medium leading-9">
      {questionIndexLabel(index, questionCount)}
      {segments.map((seg, i) =>
        seg.type === "text" ? (
          <PassageText key={i} text={seg.text} />
        ) : (
          <Input
            key={i}
            value={value[seg.index] ?? ""}
            onChange={(e) => setBlank(seg.index, e.target.value)}
            className="mx-1 inline-block h-9 w-32 align-baseline text-base md:text-base"
          />
        ),
      )}
    </div>
  );
}
