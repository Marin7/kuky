import { useTranslation } from "react-i18next";
import type { StudentQuestion } from "@/lib/placement";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { AudioPlayer } from "@/components/learning/AudioPlayer";

export interface AnswerState {
  selectedOptionIds: string[];
  answerText: string;
}

interface Props {
  question: StudentQuestion;
  index: number;
  answer: AnswerState;
  onChange: (next: AnswerState) => void;
}

/** Renders one placement question: choice/fill-blank input, plus an audio player for Listening items. */
export function PlacementQuestion({
  question: q,
  index,
  answer,
  onChange,
}: Props) {
  const { t } = useTranslation();

  const setSingle = (optionId: string) =>
    onChange({ ...answer, selectedOptionIds: [optionId] });

  const toggleMulti = (optionId: string, checked: boolean) => {
    const next = checked
      ? [...answer.selectedOptionIds, optionId]
      : answer.selectedOptionIds.filter((id) => id !== optionId);
    onChange({ ...answer, selectedOptionIds: next });
  };

  const setText = (text: string) => onChange({ ...answer, answerText: text });

  return (
    <div className="space-y-2">
      <Label className="text-sm font-medium">
        {index + 1}. {q.prompt}
      </Label>

      {(q.audioUrl || q.audioFileId) && (
        <AudioPlayer audioUrl={q.audioUrl} audioFileId={q.audioFileId} />
      )}

      {q.kind === "SINGLE_CHOICE" && (
        <RadioGroup
          value={answer.selectedOptionIds[0] ?? ""}
          onValueChange={setSingle}
        >
          {q.options.map((o) => (
            <label key={o.id} className="flex items-center gap-2 text-sm">
              <RadioGroupItem value={o.id} id={`${q.id}-${o.id}`} />
              {o.label}
            </label>
          ))}
        </RadioGroup>
      )}

      {q.kind === "MULTI_CHOICE" && (
        <div className="space-y-2">
          {q.options.map((o) => (
            <label key={o.id} className="flex items-center gap-2 text-sm">
              <Checkbox
                checked={answer.selectedOptionIds.includes(o.id)}
                onCheckedChange={(c) => toggleMulti(o.id, c === true)}
              />
              {o.label}
            </label>
          ))}
        </div>
      )}

      {q.kind === "FILL_BLANK" && (
        <Input
          value={answer.answerText}
          onChange={(e) => setText(e.target.value)}
          placeholder={t("placement.section.yourAnswer")}
          className="max-w-sm"
        />
      )}
    </div>
  );
}
