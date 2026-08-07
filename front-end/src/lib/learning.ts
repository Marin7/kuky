import { API_ORIGIN } from "@/lib/api";
import type { FormattedText } from "@/components/learning/richtext/types";
const API_BASE = `${API_ORIGIN}/api/v1`;

export type HomeworkStatus = "PENDING" | "SUBMITTED" | "REVIEWED" | "GRADED";
export type HomeworkType = "AUDIO" | "WRITE" | "GRAMMAR" | "READ";
export type HomeworkLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type HomeworkFormat = "MANUAL" | "EXERCISE";
export type QuestionKind =
  | "SINGLE_CHOICE"
  | "MULTI_CHOICE"
  | "MULTI_BLANK"
  | "DRAG_DROP"
  | "TABLE_FILL"
  | "MATCHING"
  | "TRUE_FALSE";

export interface PresentationBlock {
  heading: string;
  body: string;
}

export interface PastClass {
  id: string;
  title: string;
  heldOn: string; // ISO date, e.g. "2026-06-03"
  teacherNote: string;
}

export interface HomeworkItem {
  id: string;
  title: string;
  instructions: string;
  dueOn: string | null; // ISO date or null
  homeworkType: HomeworkType | null;
  level: HomeworkLevel | null;
  format: HomeworkFormat;
  status: HomeworkStatus;
  response: FormattedText | null;
  feedback: FormattedText | null; // teacher's formatted feedback, present once REVIEWED
  scorePercent: number | null; // present when status === "GRADED"
  submittedAt: string | null; // ISO instant or null
  overdue: boolean;
  audioUrl: string | null; // listening homework external source
  audioFileId: string | null; // listening homework uploaded file
  unit: UnitRef | null; // owning unit for grouping (null for legacy/unattached)
  unitPosition?: number | null; // rank within unit mixed sequence
  hasTeacherFeedback: boolean;
}

// --- Self-correcting exercises ---------------------------------------------

export interface StudentOption {
  id: string;
  label: string;
}

// --- Structured question payloads (student-facing — answer key stripped) ---
// See specs/024-new-exercise-types/contracts/exercise-types-api.md

export interface StudentBankItem {
  id: string;
  label: string;
}

export interface StudentTableCell {
  r: number;
  c: number;
  type: "fixed" | "blank";
  text?: string; // fixed cells only
}

export interface StudentMatchItem {
  id: string;
  label: string;
}

export interface StudentStructure {
  bank?: StudentBankItem[]; // DRAG_DROP
  rowHeaders?: string[]; // TABLE_FILL
  colHeaders?: string[]; // TABLE_FILL
  cells?: StudentTableCell[]; // TABLE_FILL
  left?: StudentMatchItem[]; // MATCHING
  right?: StudentMatchItem[]; // MATCHING
  // MULTI_BLANK carries no extra structure — blanks render from `___` in prompt.
}

export interface StudentQuestion {
  id: string;
  kind: QuestionKind;
  prompt: string;
  options: StudentOption[]; // legacy choice only; else []
  structure?: StudentStructure;
}

// --- Student answer JSON shapes by structured kind (submit payload) ---

export interface MultiBlankAnswer {
  blanks: string[];
}

export interface DragDropAnswer {
  placements: (string | null)[];
}

export interface TableFillAnswer {
  /** Key `"r,c"` for blank cells only. */
  cells: Record<string, string>;
}

export interface MatchingAnswer {
  pairs: { leftId: string; rightId: string }[];
}

export interface UnitResult {
  index: number;
  score: number; // 0 or 1
  correct: boolean;
  studentDisplay?: string | null;
  expectedDisplay?: string[] | null; // revealed when !correct
}

export interface QuestionResult {
  questionId: string;
  score: number; // 0..1
  correct: boolean;
  correctOptionIds: string[];
  acceptedAnswers: string[];
  unitResults?: UnitResult[]; // structured multi-unit kinds; empty/omitted for choice
  selectedOptionIds?: string[]; // student's choice picks
}

export interface ExerciseResult {
  scorePercent: number;
  fullyCorrectCount: number;
  totalQuestions: number;
  questions: QuestionResult[];
}

export interface ExerciseResponse {
  id: string;
  title: string;
  instructions: string;
  format: "EXERCISE";
  status: HomeworkStatus; // PENDING or GRADED
  homeworkType: HomeworkType | null;
  audioUrl: string | null; // listening homework external source
  audioFileId: string | null; // listening homework uploaded file
  questions: StudentQuestion[];
  result: ExerciseResult | null;
  teacherFeedback: string | null;
}

export interface AnswerPayload {
  questionId: string;
  selectedOptionIds: string[];
  /** Structured kinds (MULTI_BLANK/DRAG_DROP/TABLE_FILL/MATCHING); null/omitted for choice. */
  answerJson?: unknown | null;
}

export interface UnitRef {
  id: string;
  level: string;
  subject: string;
  position: number;
}

export interface PresentationFileSummary {
  id: string;
  displayName: string;
  originalName: string;
  contentType: string;
  byteSize: number;
  createdAt: string;
}

export interface SharedPresentationSummary {
  id: string;
  title: string;
  files: PresentationFileSummary[];
  unit: UnitRef | null;
  unitPosition?: number | null;
}

export interface LearningResponse {
  presentation: PresentationBlock[];
  pastClasses: PastClass[];
  homework: HomeworkItem[];
  sharedPresentations: SharedPresentationSummary[];
}

export interface ApiError {
  error: string;
  message: string;
}

async function apiCall<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (res.status === 204) return undefined as T;

  const data = await res.json();

  if (!res.ok) {
    throw data as ApiError;
  }

  return data as T;
}

/** Absolute URL of an uploaded audio file, served by the back-end. */
export const audioFileUrl = (audioFileId: string) =>
  `${API_BASE}/audio/${audioFileId}`;

export const getLearning = () => apiCall<LearningResponse>("/learning");

export const submitHomework = (
  assignmentId: string,
  response?: FormattedText | null,
) =>
  apiCall<HomeworkItem>(`/learning/homework/${assignmentId}`, {
    method: "PUT",
    body: JSON.stringify({ response: response ?? null }),
  });

export const getExercise = (assignmentId: string) =>
  apiCall<ExerciseResponse>(`/learning/homework/${assignmentId}`);

export const submitExercise = (
  assignmentId: string,
  answers: AnswerPayload[],
) =>
  apiCall<ExerciseResult>(`/learning/homework/${assignmentId}/answers`, {
    method: "PUT",
    body: JSON.stringify({ answers }),
  });

/** On-site view eligibility: only application/pdf (PPTX stays download-only). */
export const isPresentationPdf = (contentType: string): boolean =>
  contentType === "application/pdf";

export const fetchPresentationFileBlob = async (
  presentationId: string,
  fileId: string,
): Promise<Blob> => {
  const res = await fetch(
    `${API_BASE}/learning/presentations/${presentationId}/files/${fileId}`,
    {
      credentials: "include",
    },
  );
  if (!res.ok) {
    const data = await res.json();
    throw data as ApiError;
  }
  return res.blob();
};

export const downloadPresentationFile = async (
  presentationId: string,
  fileId: string,
  fileName: string,
): Promise<void> => {
  const blob = await fetchPresentationFileBlob(presentationId, fileId);
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};
