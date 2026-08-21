import type { ReactNode } from "react";
import { Label } from "@/components/ui/label";
import { displayPromptText, questionIndexLabel } from "@/lib/questionPrompt";
import { TextWithLinks } from "./TextWithLinks";

interface HeadingProps {
  index: number;
  questionCount: number;
  prompt?: string | null;
  htmlFor?: string;
}

const PROMPT_CLASS = "whitespace-pre-wrap text-base font-medium leading-9";

/** `1. Question text` — same size and line as the prompt. */
export function QuestionHeading({
  index,
  questionCount,
  prompt,
  htmlFor,
}: HeadingProps) {
  const text = displayPromptText(prompt);
  const prefix = questionIndexLabel(index, questionCount);

  if (htmlFor) {
    return (
      <Label htmlFor={htmlFor} className={`block ${PROMPT_CLASS}`}>
        {prefix}
        <TextWithLinks text={text} />
      </Label>
    );
  }
  return (
    <p className={PROMPT_CLASS}>
      {prefix}
      <TextWithLinks text={text} />
    </p>
  );
}

export function QuestionCard({ children }: { children: ReactNode }) {
  return (
    <div className="space-y-2.5 rounded-lg border bg-card p-3">{children}</div>
  );
}
