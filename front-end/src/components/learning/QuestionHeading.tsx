import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Label } from "@/components/ui/label";
import { displayPromptText } from "@/lib/questionPrompt";

interface HeadingProps {
  index: number;
  /** Prompt shown on the line below the heading. Omit when the prompt is inline. */
  prompt?: string | null;
  htmlFor?: string;
}

/** "Pregunta N" on its own line, then the prompt without a leading `N.` */
export function QuestionHeading({ index, prompt, htmlFor }: HeadingProps) {
  const { t } = useTranslation();
  const text = displayPromptText(prompt);
  const title = t("learning.manualMulti.questionLabel", { index });

  return (
    <div className="space-y-1">
      {htmlFor ? (
        <Label htmlFor={htmlFor} className="block text-sm font-semibold">
          {title}
        </Label>
      ) : (
        <p className="text-sm font-semibold text-foreground">{title}</p>
      )}
      {text ? (
        <p className="whitespace-pre-wrap text-base font-medium leading-relaxed">
          {text}
        </p>
      ) : null}
    </div>
  );
}

export function QuestionCard({ children }: { children: ReactNode }) {
  return (
    <div className="space-y-2.5 rounded-lg border bg-card p-3">{children}</div>
  );
}
