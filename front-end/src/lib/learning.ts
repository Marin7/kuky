import { API_ORIGIN } from "@/lib/api";
const API_BASE = `${API_ORIGIN}/api/v1`;

export type HomeworkStatus = "PENDING" | "SUBMITTED" | "REVIEWED" | "GRADED";
export type HomeworkType = "AUDIO" | "WRITE" | "GRAMMAR" | "READ";
export type HomeworkLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type HomeworkFormat = "MANUAL" | "EXERCISE";
export type QuestionKind = "SINGLE_CHOICE" | "MULTI_CHOICE" | "FILL_BLANK";

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
  response: string | null;
  scorePercent: number | null; // present when status === "GRADED"
  submittedAt: string | null; // ISO instant or null
  overdue: boolean;
  audioUrl: string | null; // listening homework external source
  audioFileId: string | null; // listening homework uploaded file
}

// --- Self-correcting exercises ---------------------------------------------

export interface StudentOption {
  id: string;
  label: string;
}

export interface StudentQuestion {
  id: string;
  kind: QuestionKind;
  prompt: string;
  options: StudentOption[]; // empty for FILL_BLANK
}

export interface QuestionResult {
  questionId: string;
  score: number; // 0..1
  correct: boolean;
  correctOptionIds: string[];
  acceptedAnswers: string[];
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
}

export interface AnswerPayload {
  questionId: string;
  selectedOptionIds: string[];
  answerText: string | null;
}

export interface SharedPresentationSummary {
  id: string;
  title: string;
  hasFile: boolean;
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

export const submitHomework = (assignmentId: string, response?: string) =>
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

export const downloadPresentation = async (
  id: string,
  fileName: string,
): Promise<void> => {
  const res = await fetch(`${API_BASE}/learning/presentations/${id}/file`, {
    credentials: "include",
  });
  if (!res.ok) {
    const data = await res.json();
    throw data as ApiError;
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};
