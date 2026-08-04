import { splitPromptSegments } from "@/lib/blankTokens";
import { Input } from "@/components/ui/input";
import { PassageText } from "./PassageText";

interface Props {
  prompt: string;
  value: string[];
  onChange: (blanks: string[]) => void;
}

/** Renders a MULTI_BLANK passage with an inline input at each `___` token. */
export function MultiBlankQuestion({ prompt, value, onChange }: Props) {
  const segments = splitPromptSegments(prompt);

  const setBlank = (index: number, text: string) => {
    const next = [...value];
    next[index] = text;
    onChange(next);
  };

  return (
    <div className="text-sm leading-9">
      {segments.map((seg, i) =>
        seg.type === "text" ? (
          <PassageText key={i} text={seg.text} />
        ) : (
          <Input
            key={i}
            value={value[seg.index] ?? ""}
            onChange={(e) => setBlank(seg.index, e.target.value)}
            className="mx-1 inline-block h-8 w-28 align-baseline"
          />
        ),
      )}
    </div>
  );
}
