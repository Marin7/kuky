import { genId } from "@/lib/utils";
import type {
  AdminOption,
  AdminQuestion,
  QuestionKind,
  QuestionStructure,
} from "@/lib/admin";

/** Kinds that store their answer key in `structure` instead of `options` rows. */
export function isStructuredKind(kind: QuestionKind): boolean {
  return (
    kind === "MULTI_BLANK" ||
    kind === "DRAG_DROP" ||
    kind === "TABLE_FILL" ||
    kind === "MATCHING"
  );
}

export function defaultOptionsForKind(kind: QuestionKind): AdminOption[] {
  if (isStructuredKind(kind)) return [];
  if (kind === "FILL_BLANK") return [{ label: "", correct: true }];
  return [
    { label: "", correct: false },
    { label: "", correct: false },
  ];
}

export function defaultStructureForKind(kind: QuestionKind): QuestionStructure {
  switch (kind) {
    case "MULTI_BLANK":
      return { blanks: [] };
    case "DRAG_DROP":
      return { bank: [] };
    case "TABLE_FILL":
      return {
        rowHeaders: [""],
        colHeaders: [""],
        cells: [{ r: 0, c: 0, type: "blank", acceptedAnswers: [""] }],
      };
    case "MATCHING":
      return {
        left: [{ id: genId(), label: "" }],
        right: [{ id: genId(), label: "" }],
        pairs: [],
      };
    default:
      return {};
  }
}

export function defaultQuestion(
  kind: QuestionKind = "SINGLE_CHOICE",
): AdminQuestion {
  return {
    kind,
    prompt: "",
    options: defaultOptionsForKind(kind),
    structure: defaultStructureForKind(kind),
  };
}
