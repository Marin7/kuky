import { API_ORIGIN } from "@/lib/api";
import type {
  HeterogeneousAnswerPayload,
  MediaSourceKind,
  QuestionKind,
  StudentQuestion,
  StudentStructure,
} from "@/lib/learning";

const API_BASE = `${API_ORIGIN}/api/v1`;

export type QuizSkill = "READING" | "WRITING" | "GRAMMAR" | "LISTENING";
export type QuizStatus = "AVAILABLE" | "IN_PROGRESS" | "SUBMITTED" | "GRADED";

export interface QuizListItem {
  id: string;
  title: string;
  description: string | null;
  status: QuizStatus;
  unseen?: boolean;
}

export interface QuizStudentQuestion extends StudentQuestion {
  skill: QuizSkill;
  mediaSourceKind?: MediaSourceKind | null;
  audioUrl?: string | null;
  audioFileId?: string | null;
}

export interface QuizSkillScore {
  skill: QuizSkill;
  scorePercent: number | null;
  fullyCorrectCount: number | null;
  questionUnitCount: number;
  awaitingTeacher: boolean;
}

export interface QuizQuestionResult {
  questionId: string;
  skill: QuizSkill;
  score: number;
  correct: boolean;
  correctOptionIds: string[];
  acceptedAnswers: string[];
  unitResults: {
    index: number;
    score: number;
    correct: boolean;
    studentDisplay?: string | null;
    expectedDisplay?: string[] | null;
  }[];
  selectedOptionIds: string[];
  answerText?: string | null;
  teacherPercent?: number | null;
  formatted?: import("@/components/learning/richtext/types").FormattedText | null;
}

export interface QuizTakeResponse {
  id: string;
  title: string;
  description: string | null;
  status: QuizStatus;
  questions: QuizStudentQuestion[];
  scorePercent: number | null;
  fullyCorrectCount: number | null;
  questionUnitCount: number | null;
  skills: QuizSkillScore[];
  results: QuizQuestionResult[];
  feedback: string | null;
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
  if (!res.ok) throw data as ApiError;
  return data as T;
}

export const listMyQuizzes = () =>
  apiCall<{ quizzes: QuizListItem[] }>("/quizzes").then((r) => r.quizzes);

export const getQuiz = (id: string) => apiCall<QuizTakeResponse>(`/quizzes/${id}`);

export const submitQuizAnswers = (
  id: string,
  answers: HeterogeneousAnswerPayload[],
) =>
  apiCall<QuizTakeResponse>(`/quizzes/${id}/answers`, {
    method: "PUT",
    body: JSON.stringify({ answers }),
  });

export type { QuestionKind, StudentStructure };
