import { useTranslation } from "react-i18next";
import type {
  AdminQuestion,
  AdminOption,
  QuestionKind,
  MultiBlankStructure,
  DragDropStructure,
  TableFillStructure,
  MatchingStructure,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  defaultOptionsForKind,
  defaultStructureForKind,
  isStructuredKind,
} from "./questionDefaults";
import { MultiBlankEditor } from "./MultiBlankEditor";
import { DragDropEditor } from "./DragDropEditor";
import { TableFillEditor } from "./TableFillEditor";
import { MatchingEditor } from "./MatchingEditor";

const KINDS: QuestionKind[] = [
  "SINGLE_CHOICE",
  "MULTI_CHOICE",
  "FILL_BLANK",
  "MULTI_BLANK",
  "DRAG_DROP",
  "TABLE_FILL",
  "MATCHING",
];

interface Props {
  index: number;
  question: AdminQuestion;
  count: number;
  onChange: (q: AdminQuestion) => void;
  onRemove: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}

export function QuestionEditorCard({
  index,
  question,
  count,
  onChange,
  onRemove,
  onMoveUp,
  onMoveDown,
}: Props) {
  const { t } = useTranslation();

  const setKind = (kind: QuestionKind) => {
    onChange({
      ...question,
      kind,
      options: defaultOptionsForKind(kind),
      structure: defaultStructureForKind(kind),
    });
  };

  const setPrompt = (prompt: string) => onChange({ ...question, prompt });

  const setOption = (i: number, patch: Partial<AdminOption>) => {
    const options = question.options.map((o, idx) =>
      idx === i ? { ...o, ...patch } : o,
    );
    onChange({ ...question, options });
  };

  const setSingleCorrect = (i: number) => {
    const options = question.options.map((o, idx) => ({
      ...o,
      correct: idx === i,
    }));
    onChange({ ...question, options });
  };

  const addOption = () =>
    onChange({
      ...question,
      options: [...question.options, { label: "", correct: false }],
    });

  const removeOption = (i: number) =>
    onChange({
      ...question,
      options: question.options.filter((_, idx) => idx !== i),
    });

  const isFillBlank = question.kind === "FILL_BLANK";
  const structured = isStructuredKind(question.kind);
  const isPassageKind =
    question.kind === "MULTI_BLANK" || question.kind === "DRAG_DROP";
  const singleCorrectIndex = question.options.findIndex((o) => o.correct);

  return (
    <div className="space-y-3 rounded-lg border bg-card p-4">
      <div className="flex items-center justify-between gap-2">
        <span className="text-sm font-semibold">
          {t("admin.homework.questions.questionLabel", { index: index + 1 })}
        </span>
        <div className="flex items-center gap-1">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            disabled={index === 0}
            onClick={onMoveUp}
          >
            ↑
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            disabled={index === count - 1}
            onClick={onMoveDown}
          >
            ↓
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs text-destructive"
            onClick={onRemove}
          >
            {t("admin.homework.questions.remove")}
          </Button>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-[180px_1fr]">
        <div className="space-y-1">
          <Label>{t("admin.homework.questions.kindLabel")}</Label>
          <Select
            value={question.kind}
            onValueChange={(v) => setKind(v as QuestionKind)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {KINDS.map((v) => (
                <SelectItem key={v} value={v}>
                  {t(`admin.homework.questions.kind.${v}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label>{t("admin.homework.questions.promptLabel")}</Label>
          <Textarea
            value={question.prompt}
            onChange={(e) => setPrompt(e.target.value)}
            rows={isPassageKind ? 3 : 2}
            placeholder={t("admin.homework.questions.promptPlaceholder")}
          />
          <p className="text-xs text-muted-foreground">
            {isPassageKind
              ? t("admin.homework.questions.promptBlanksHint")
              : question.kind === "TABLE_FILL" || question.kind === "MATCHING"
                ? t("admin.homework.questions.promptOptionalHint")
                : null}
          </p>
        </div>
      </div>

      {question.kind === "MULTI_BLANK" && (
        <MultiBlankEditor
          prompt={question.prompt}
          structure={
            (question.structure as MultiBlankStructure) ?? { blanks: [] }
          }
          onChange={(structure) => onChange({ ...question, structure })}
        />
      )}

      {question.kind === "DRAG_DROP" && (
        <DragDropEditor
          prompt={question.prompt}
          structure={(question.structure as DragDropStructure) ?? { bank: [] }}
          onChange={(structure) => onChange({ ...question, structure })}
        />
      )}

      {question.kind === "TABLE_FILL" && (
        <TableFillEditor
          structure={
            (question.structure as TableFillStructure) ?? {
              rowHeaders: [],
              colHeaders: [],
              cells: [],
            }
          }
          onChange={(structure) => onChange({ ...question, structure })}
        />
      )}

      {question.kind === "MATCHING" && (
        <MatchingEditor
          structure={
            (question.structure as MatchingStructure) ?? {
              left: [],
              right: [],
              pairs: [],
            }
          }
          onChange={(structure) => onChange({ ...question, structure })}
        />
      )}

      {!structured &&
        (isFillBlank ? (
          <div className="space-y-2">
            <Label>{t("admin.homework.questions.acceptedAnswers")}</Label>
            <p className="text-xs text-muted-foreground">
              {t("admin.homework.questions.acceptedAnswersHint")}
            </p>
            {question.options.map((o, i) => (
              <div key={i} className="flex items-center gap-2">
                <Input
                  value={o.label}
                  onChange={(e) =>
                    setOption(i, { label: e.target.value, correct: true })
                  }
                  placeholder={t(
                    "admin.homework.questions.acceptedAnswerPlaceholder",
                  )}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2 text-xs text-destructive"
                  disabled={question.options.length === 1}
                  onClick={() => removeOption(i)}
                >
                  ✕
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-8 text-xs"
              onClick={addOption}
            >
              {t("admin.homework.questions.addAnswer")}
            </Button>
          </div>
        ) : (
          <div className="space-y-2">
            <Label>{t("admin.homework.questions.optionsLabel")}</Label>
            <p className="text-xs text-muted-foreground">
              {question.kind === "SINGLE_CHOICE"
                ? t("admin.homework.questions.singleHint")
                : t("admin.homework.questions.multiHint")}
            </p>
            {question.kind === "SINGLE_CHOICE" ? (
              <RadioGroup
                value={
                  singleCorrectIndex >= 0 ? String(singleCorrectIndex) : ""
                }
                onValueChange={(v) => setSingleCorrect(Number(v))}
              >
                {question.options.map((o, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <RadioGroupItem value={String(i)} id={`q${index}-o${i}`} />
                    <Input
                      value={o.label}
                      onChange={(e) => setOption(i, { label: e.target.value })}
                      placeholder={`Opción ${i + 1}`}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-8 px-2 text-xs text-destructive"
                      disabled={question.options.length <= 2}
                      onClick={() => removeOption(i)}
                    >
                      ✕
                    </Button>
                  </div>
                ))}
              </RadioGroup>
            ) : (
              question.options.map((o, i) => (
                <div key={i} className="flex items-center gap-2">
                  <Checkbox
                    checked={o.correct}
                    onCheckedChange={(c) =>
                      setOption(i, { correct: c === true })
                    }
                  />
                  <Input
                    value={o.label}
                    onChange={(e) => setOption(i, { label: e.target.value })}
                    placeholder={`Opción ${i + 1}`}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-8 px-2 text-xs text-destructive"
                    disabled={question.options.length <= 2}
                    onClick={() => removeOption(i)}
                  >
                    ✕
                  </Button>
                </div>
              ))
            )}
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-8 text-xs"
              onClick={addOption}
            >
              {t("admin.homework.questions.addOption")}
            </Button>
          </div>
        ))}
    </div>
  );
}
