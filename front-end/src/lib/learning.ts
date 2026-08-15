import { API_ORIGIN } from "@/lib/api";
import type { FormattedText } from "@/components/learning/richtext/types";
const API_BASE = `${API_ORIGIN}/api/v1`;

export type HomeworkStatus = "PENDING" | "SUBMITTED" | "REVIEWED" | "GRADED";

/** Student may edit answers only while the submission is still PENDING. */
export function isStudentHomeworkEditable(status: HomeworkStatus): boolean {
  return status === "PENDING";
}
export type HomeworkType = "AUDIO" | "WRITE" | "GRAMMAR" | "READ";
export type HomeworkLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type HomeworkFormat = "MANUAL" | "EXERCISE" | "MIXED";
/** Preferred UI discriminator — derived server-side from type + question kinds. */
export type HomeworkComposition = "WRITE" | "ALL_MANUAL" | "ALL_AUTO" | "MIXED";
export type MediaSourceKind =
  | "AUDIO_URL"
  | "UPLOADED_FILE"
  | "VIDEO_PAGE"
  | "YOUTUBE";
export type QuestionKind =
  | "SINGLE_CHOICE"
  | "MULTI_CHOICE"
  | "MULTI_BLANK"
  | "DRAG_DROP"
  | "TABLE_FILL"
  | "MATCHING"
  | "TRUE_FALSE"
  | "FREE_TEXT";

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

export interface ManualAnswerItem {
  questionId: string;
  promptSnapshot: string;
  text: string;
  formatted?: FormattedText | null;
  kind?: QuestionKind | null;
  /** Present after GRADED; omitted while SUBMITTED even if teacher saved drafts. */
  teacherScorePercent?: number | null;
  score?: number | null;
  correct?: boolean | null;
}

export interface ManualAnswerPayload {
  questionId: string;
  text: string;
}

export interface HomeworkItem {
  id: string;
  title: string;
  instructions: string;
  dueOn: string | null; // ISO date or null
  homeworkType: HomeworkType | null;
  level: HomeworkLevel | null;
  format: HomeworkFormat;
  /** Preferred UI discriminator; fall back via resolveComposition(). */
  composition?: HomeworkComposition | null;
  status: HomeworkStatus;
  response: FormattedText | null;
  feedback: FormattedText | null; // LEGACY_RICH teacher feedback
  feedbackText?: string | null; // ANNOTATED plain note
  reviewModel?: "LEGACY_RICH" | "ANNOTATED" | null;
  scorePercent: number | null; // present when status === "GRADED"
  /** MIXED awaiting teacher: auto-only mean; null once finalized. */
  provisionalScorePercent?: number | null;
  submittedAt: string | null; // ISO instant or null
  overdue: boolean;
  audioUrl: string | null; // listening homework external source
  audioFileId: string | null; // listening homework uploaded file
  mediaSourceKind?: MediaSourceKind | null;
  unit: UnitRef | null; // owning unit for grouping (null for legacy/unattached)
  unitPosition?: number | null; // rank within unit mixed sequence
  hasTeacherFeedback: boolean;
  /** Non-WRITE questions (manual, auto, or mixed). */
  questions?: StudentQuestion[];
  /** Submitted answers (FREE_TEXT + optional auto metadata). */
  answers?: ManualAnswerItem[] | null;
  /** Auto-graded subset results (ALL_AUTO / MIXED after submit). */
  result?: ExerciseResult | null;
  teacherFeedback?: string | null;
  /** Present while PENDING; echo on submit. Omitted after submit. */
  contentRevisedAt?: string | null;
  /** Newly assigned homework not yet opened by the student. */
  unseen?: boolean;
}

/** Resolve composition preferring the server field, else format / questions. */
export function resolveComposition(item: {
  composition?: HomeworkComposition | null;
  format?: HomeworkFormat | null;
  homeworkType?: HomeworkType | null;
  questions?: { kind: QuestionKind }[] | null;
}): HomeworkComposition {
  if (item.composition) return item.composition;
  if (item.homeworkType === "WRITE") return "WRITE";
  if (item.format === "MIXED") return "MIXED";
  if (item.format === "EXERCISE") return "ALL_AUTO";
  if (item.format === "MANUAL") return "ALL_MANUAL";
  const qs = item.questions ?? [];
  if (qs.length === 0) return "ALL_MANUAL";
  const hasFree = qs.some((q) => q.kind === "FREE_TEXT");
  const hasAuto = qs.some((q) => q.kind !== "FREE_TEXT");
  if (hasFree && hasAuto) return "MIXED";
  if (hasAuto) return "ALL_AUTO";
  return "ALL_MANUAL";
}

export function isAutoTakeComposition(c: HomeworkComposition): boolean {
  return c === "ALL_AUTO" || c === "MIXED";
}

/** AUDIO homework is incomplete when mediaSourceKind is missing (FR-001b). */
export function isListeningMediaReady(item: {
  homeworkType?: HomeworkType | null;
  mediaSourceKind?: MediaSourceKind | null;
}): boolean {
  if (item.homeworkType !== "AUDIO") return true;
  return item.mediaSourceKind != null;
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

export interface StudentSingleChoiceItemOption {
  id: string;
  label: string;
}

export interface StudentSingleChoiceItem {
  number: number;
  options: StudentSingleChoiceItemOption[];
}

export interface StudentStructure {
  bank?: StudentBankItem[]; // DRAG_DROP
  rowHeaders?: string[]; // TABLE_FILL
  colHeaders?: string[]; // TABLE_FILL
  cells?: StudentTableCell[]; // TABLE_FILL
  left?: StudentMatchItem[]; // MATCHING
  right?: StudentMatchItem[]; // MATCHING
  items?: StudentSingleChoiceItem[]; // numbered SINGLE_CHOICE (no `correct`)
  // MULTI_BLANK carries no extra structure — blanks render from `___` in prompt.
}

export interface StudentQuestion {
  id: string;
  kind: QuestionKind;
  prompt: string;
  options: StudentOption[]; // legacy choice only; else []
  structure?: StudentStructure;
  skill?: string;
  mediaSourceKind?: MediaSourceKind | null;
  audioUrl?: string | null;
  audioFileId?: string | null;
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

/** Numbered SINGLE_CHOICE: option id per item number (`"1"` → option id). */
export interface SingleChoiceSelectionsAnswer {
  selections: Record<string, string>;
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
  format: HomeworkFormat;
  composition?: HomeworkComposition | null;
  status: HomeworkStatus;
  homeworkType: HomeworkType | null;
  audioUrl: string | null; // listening homework external source
  audioFileId: string | null; // listening homework uploaded file
  mediaSourceKind?: MediaSourceKind | null;
  questions: StudentQuestion[];
  result: ExerciseResult | null;
  /** FREE_TEXT answers for MIXED (and optional metadata). */
  answers?: ManualAnswerItem[] | null;
  scorePercent?: number | null;
  provisionalScorePercent?: number | null;
  feedbackText?: string | null;
  feedback?: FormattedText | null;
  teacherFeedback: string | null;
  /** Present while PENDING; echo on submit. Omitted after submit. */
  contentRevisedAt?: string | null;
}

export interface AnswerPayload {
  questionId: string;
  selectedOptionIds?: string[];
  /** Structured kinds (MULTI_BLANK/DRAG_DROP/TABLE_FILL/MATCHING); null/omitted for choice. */
  answerJson?: unknown | null;
  /** FREE_TEXT answers on mixed /answers submit. */
  text?: string;
  /** FREE_TEXT rich text (writing homework parity). */
  formatted?: FormattedText | null;
}

/** Heterogeneous payload for PUT .../answers (structured + FREE_TEXT). */
export type HeterogeneousAnswerPayload = AnswerPayload;

export interface UnitRef {
  id: string;
  level: string;
  subject: string;
  position: number;
  unseen?: boolean;
}

export interface PresentationFileSummary {
  id: string;
  displayName: string;
  originalName: string;
  contentType: string;
  byteSize: number;
  createdAt: string;
}

export interface ActivitySummary {
  id: string;
  title: string;
  format: HomeworkFormat;
  composition?: HomeworkComposition | null;
  position: number;
  status: HomeworkItem["status"];
  scorePercent: number | null;
  triggerFileId: string | null;
  /** Insert the activity after this PDF page (between N and N+1). */
  triggerPage: number | null;
  instructionsText: string;
  youtubeUrl: string | null;
  imageId: string | null;
}

export interface SharedPresentationSummary {
  id: string;
  title: string;
  files: PresentationFileSummary[];
  unit: UnitRef | null;
  unitPosition?: number | null;
  activities?: ActivitySummary[];
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
  answers?: ManualAnswerPayload[],
  contentRevisedAt?: string | null,
) =>
  apiCall<HomeworkItem>(`/learning/homework/${assignmentId}`, {
    method: "PUT",
    body: JSON.stringify(
      answers != null
        ? { answers, contentRevisedAt: contentRevisedAt ?? null }
        : {
            response: response ?? null,
            contentRevisedAt: contentRevisedAt ?? null,
          },
    ),
  });

export const getExercise = (assignmentId: string) =>
  apiCall<ExerciseResponse>(`/learning/homework/${assignmentId}`);

/** ALL_AUTO submit — returns nested ExerciseResult for ExerciseForm compatibility. */
export const submitExercise = async (
  assignmentId: string,
  answers: HeterogeneousAnswerPayload[],
  contentRevisedAt?: string | null,
): Promise<ExerciseResult> => {
  const item = await apiCall<HomeworkItem>(
    `/learning/homework/${assignmentId}/answers`,
    {
      method: "PUT",
      body: JSON.stringify({
        answers,
        contentRevisedAt: contentRevisedAt ?? null,
      }),
    },
  );
  if (!item.result) {
    throw { error: "VALIDATION_ERROR", message: "Missing exercise result" };
  }
  return item.result;
};

/** Unified non-WRITE submit (ALL_AUTO / ALL_MANUAL / MIXED) via /answers. */
export const submitHomeworkAnswers = (
  assignmentId: string,
  answers: HeterogeneousAnswerPayload[],
  contentRevisedAt?: string | null,
) =>
  apiCall<HomeworkItem>(`/learning/homework/${assignmentId}/answers`, {
    method: "PUT",
    body: JSON.stringify({
      answers,
      contentRevisedAt: contentRevisedAt ?? null,
    }),
  });

export const isHomeworkUpdatedError = (e: unknown): boolean =>
  typeof e === "object" &&
  e !== null &&
  (e as ApiError).error === "HOMEWORK_UPDATED";

export const homeworkDraftKey = (homeworkId: string) =>
  `kuky:homework-draft:${homeworkId}`;

export function clearHomeworkDraft(homeworkId: string) {
  if (typeof window === "undefined") return;
  try {
    localStorage.removeItem(homeworkDraftKey(homeworkId));
  } catch {
    // ignore
  }
}

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

// ---------------------------------------------------------------------------
// Presentation Activities (student)
// ---------------------------------------------------------------------------

export interface ActivityItem {
  id: string;
  title: string;
  format: HomeworkFormat;
  composition?: HomeworkComposition | null;
  status: HomeworkStatus;
  level: HomeworkLevel | null;
  homeworkType: HomeworkType | null;
  triggerFileId: string | null;
  triggerPage: number | null;
  instructionsText: string;
  youtubeUrl: string | null;
  imageId: string | null;
  response: FormattedText | null;
  feedback: FormattedText | null;
  feedbackText?: string | null;
  reviewModel?: "LEGACY_RICH" | "ANNOTATED" | null;
  scorePercent: number | null;
  provisionalScorePercent?: number | null;
  questions: StudentQuestion[];
  /** Submitted answers (FREE_TEXT + optional auto metadata). */
  answers?: ManualAnswerItem[] | null;
  result: ExerciseResult | null;
  teacherFeedback: string | null;
}

export const getActivity = (id: string) =>
  apiCall<ActivityItem>(`/learning/activities/${id}`);

export const submitActivity = (
  id: string,
  response?: FormattedText | null,
  answers?: ManualAnswerPayload[],
) =>
  apiCall<ActivityItem>(`/learning/activities/${id}`, {
    method: "PUT",
    body: JSON.stringify(
      answers != null ? { answers } : { response: response ?? null },
    ),
  });

/** ALL_AUTO activity submit — returns nested ExerciseResult for ExerciseForm. */
export const submitActivityAnswers = async (
  id: string,
  answers: HeterogeneousAnswerPayload[],
): Promise<ExerciseResult> => {
  const item = await apiCall<ActivityItem>(
    `/learning/activities/${id}/answers`,
    {
      method: "PUT",
      body: JSON.stringify({ answers }),
    },
  );
  if (!item.result) {
    throw { error: "VALIDATION_ERROR", message: "Missing exercise result" };
  }
  return item.result;
};

/** Activity mixed/unified submit returning the full item (provisional/final). */
export const submitActivityAnswersItem = (
  id: string,
  answers: HeterogeneousAnswerPayload[],
) =>
  apiCall<ActivityItem>(`/learning/activities/${id}/answers`, {
    method: "PUT",
    body: JSON.stringify({ answers }),
  });
