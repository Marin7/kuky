import {
  type AdminQuestion,
  type DragDropStructure,
  type HomeworkAdminItem,
  type MatchingStructure,
  type SingleChoiceStructure,
  type TableFillStructure,
} from "@/lib/admin";
import {
  pinInstructionsAboveWordBank,
  resolveComposition,
  type StudentQuestion,
  type StudentStructure,
} from "@/lib/learning";
import { matchClassicInlineSingleChoice } from "@/lib/inlineChoice";
import { AudioPlayer } from "@/components/learning/AudioPlayer";
import { TextWithLinks } from "@/components/learning/TextWithLinks";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { DragDropQuestion } from "@/components/learning/DragDropQuestion";
import { InlineSingleChoiceQuestion } from "@/components/learning/InlineSingleChoiceQuestion";
import { MatchingQuestion } from "@/components/learning/MatchingQuestion";
import { MultiBlankQuestion } from "@/components/learning/MultiBlankQuestion";
import { NumberedSingleChoiceQuestion } from "@/components/learning/NumberedSingleChoiceQuestion";
import {
  QuestionCard,
  QuestionHeading,
} from "@/components/learning/QuestionHeading";
import { TableFillQuestion } from "@/components/learning/TableFillQuestion";

function numberedItems(q: StudentQuestion) {
  return q.kind === "SINGLE_CHOICE" ? (q.structure?.items ?? []) : [];
}

function hidesPromptLabel(
  kind: StudentQuestion["kind"],
  inlineChoice: ReturnType<typeof matchClassicInlineSingleChoice>,
  numbered = false,
): boolean {
  return (
    kind === "MULTI_BLANK" ||
    kind === "DRAG_DROP" ||
    inlineChoice != null ||
    numbered
  );
}

function toStudentStructure(
  q: AdminQuestion,
  questionId: string,
): StudentStructure | undefined {
  const s = q.structure;
  if (!s || Object.keys(s).length === 0) return undefined;

  if (q.kind === "DRAG_DROP") {
    const d = s as DragDropStructure;
    return {
      bank: (d.bank ?? []).map((b) => ({ id: b.id, label: b.label })),
      bankReusable:
        "bankReusable" in d
          ? Boolean((d as { bankReusable?: boolean }).bankReusable)
          : undefined,
    };
  }
  if (q.kind === "TABLE_FILL") {
    const t = s as TableFillStructure;
    return {
      rowHeaders: t.rowHeaders,
      colHeaders: t.colHeaders,
      cells: (t.cells ?? []).map((c) => ({
        r: c.r,
        c: c.c,
        type: c.type,
        text: c.type === "fixed" ? c.text : undefined,
      })),
    };
  }
  if (q.kind === "MATCHING") {
    const m = s as MatchingStructure;
    return { left: m.left ?? [], right: m.right ?? [] };
  }
  if (q.kind === "SINGLE_CHOICE") {
    const sc = s as SingleChoiceStructure;
    if (!sc.items?.length) return undefined;
    return {
      items: sc.items.map((item) => ({
        number: item.number,
        options: item.options.map((o, i) => ({
          id: o.id ?? `${questionId}-${item.number}-${i}`,
          label: o.label,
        })),
      })),
    };
  }
  return undefined;
}

function toPreviewQuestions(questions: AdminQuestion[]): StudentQuestion[] {
  return questions.map((q, index) => {
    const id = q.id ?? `preview-${index}`;
    return {
      id,
      kind: q.kind,
      prompt: q.prompt,
      options: (q.options ?? []).map((o, i) => ({
        id: o.id ?? `${id}-opt-${i}`,
        label: o.label,
      })),
      structure: toStudentStructure(q, id),
    };
  });
}

const noop = () => {};

interface Props {
  item: HomeworkAdminItem;
}

/** Uneditable student-like take of a homework (no answer textboxes). */
export function HomeworkPreview({ item }: Props) {
  const composition = resolveComposition(item);
  const questions = toPreviewQuestions(item.questions ?? []);
  const pinIntro = pinInstructionsAboveWordBank(questions, item.homeworkType);
  const showPassageBox = item.homeworkType === "READ";
  const bankQuestionId = questions.find((q) => q.kind === "DRAG_DROP")?.id;
  const questionCount = questions.length;
  const showQuestions = composition !== "WRITE" && questionCount > 0;

  return (
    <div className="space-y-4">
      {item.instructions &&
        !pinIntro &&
        (showPassageBox ? (
          <div className="whitespace-pre-wrap rounded-lg border bg-card p-4 text-base leading-relaxed text-foreground">
            <TextWithLinks text={item.instructions} />
          </div>
        ) : (
          <p className="whitespace-pre-wrap text-base leading-relaxed text-foreground">
            <TextWithLinks text={item.instructions} />
          </p>
        ))}
      {(item.audioUrl || item.audioFileId) && (
        <AudioPlayer
          mediaSourceKind={item.mediaSourceKind}
          audioUrl={item.audioUrl}
          audioFileId={item.audioFileId}
        />
      )}
      {showQuestions && (
        <div className="space-y-3">
          {questions.map((q, i) => {
            const inlineChoice = matchClassicInlineSingleChoice(q);
            const numbered = numberedItems(q).length > 0;
            const hidePrompt = hidesPromptLabel(q.kind, inlineChoice, numbered);
            const body = (
              <>
                {numbered && (
                  <NumberedSingleChoiceQuestion
                    index={i + 1}
                    questionCount={questionCount}
                    prompt={q.prompt}
                    items={numberedItems(q)}
                    selections={{}}
                    onChange={noop}
                    readOnly
                  />
                )}

                {inlineChoice && (
                  <InlineSingleChoiceQuestion
                    index={i + 1}
                    questionCount={questionCount}
                    prompt={q.prompt}
                    match={inlineChoice}
                    selectedOptionId={null}
                    onChange={noop}
                    readOnly
                  />
                )}

                {q.kind === "SINGLE_CHOICE" && !numbered && !inlineChoice && (
                  <RadioGroup value="" disabled>
                    {q.options.map((o) => (
                      <label
                        key={o.id}
                        className="flex items-center gap-2.5 text-base leading-snug"
                      >
                        <RadioGroupItem
                          value={o.id}
                          id={`preview-${q.id}-${o.id}`}
                          disabled
                        />
                        {o.label}
                      </label>
                    ))}
                  </RadioGroup>
                )}

                {q.kind === "MULTI_CHOICE" && (
                  <div className="space-y-2.5">
                    {q.options.map((o) => (
                      <label
                        key={o.id}
                        className="flex items-center gap-2.5 text-base leading-snug"
                      >
                        <Checkbox disabled />
                        {o.label}
                      </label>
                    ))}
                  </div>
                )}

                {q.kind === "MULTI_BLANK" && (
                  <MultiBlankQuestion
                    index={i + 1}
                    questionCount={questionCount}
                    prompt={q.prompt}
                    value={[]}
                    onChange={noop}
                    readOnly
                  />
                )}

                {q.kind === "DRAG_DROP" && (
                  <DragDropQuestion
                    index={i + 1}
                    questionCount={questionCount}
                    prompt={q.prompt}
                    bank={q.structure?.bank ?? []}
                    bankReusable={q.structure?.bankReusable === true}
                    value={[]}
                    onChange={noop}
                    intro={
                      pinIntro && q.id === bankQuestionId
                        ? item.instructions
                        : null
                    }
                    readOnly
                  />
                )}

                {q.kind === "TABLE_FILL" && (
                  <TableFillQuestion
                    structure={q.structure ?? {}}
                    value={{}}
                    onChange={noop}
                    readOnly
                  />
                )}

                {q.kind === "MATCHING" && (
                  <MatchingQuestion
                    left={q.structure?.left ?? []}
                    right={q.structure?.right ?? []}
                    pairs={[]}
                    onChange={noop}
                    readOnly
                  />
                )}
              </>
            );

            return (
              <QuestionCard key={q.id}>
                {q.kind === "FREE_TEXT" || !hidePrompt ? (
                  <QuestionHeading
                    index={i + 1}
                    questionCount={questionCount}
                    prompt={q.prompt}
                  />
                ) : null}
                {q.kind !== "FREE_TEXT" ? body : null}
              </QuestionCard>
            );
          })}
        </div>
      )}
    </div>
  );
}
