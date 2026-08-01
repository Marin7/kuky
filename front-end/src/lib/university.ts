import { API_ORIGIN } from "@/lib/api";
import type {
  AnswerPayload,
  ExerciseResponse,
  ExerciseResult,
  HomeworkFormat,
  HomeworkStatus,
} from "@/lib/learning";
import type { FormattedText } from "@/components/learning/richtext/types";

const API_BASE = `${API_ORIGIN}/api/v1/university`;

export type UniversityLevel = "BEGINNER" | "INTERMEDIATE";

export interface UniversitySession {
  id: string;
  level: UniversityLevel;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  title: string | null;
}

export interface UniversitySchedule {
  viewerMode: "FULL_LABELED" | "LEVEL_FILTERED";
  level: UniversityLevel | null;
  templateSessions: UniversitySession[];
  exceptions: Array<{
    id: string;
    level: UniversityLevel;
    exceptionDate: string;
    kind: "CANCEL" | "EXTRA";
    sessionId: string | null;
    startTime: string | null;
    endTime: string | null;
    title: string | null;
  }>;
}

export interface UniversityExam {
  id: string;
  title: string;
  examAt: string;
  description: string | null;
}

export interface UniversityNews {
  id: string;
  title: string;
  body: string;
  publishedAt: string;
}

export interface UniversityHomework {
  id: string;
  title: string;
  instructions?: string;
  format: HomeworkFormat;
  status: HomeworkStatus;
  dueOn: string | null;
}

export interface UniversityLearning {
  level: UniversityLevel;
  presentations: Array<{ id: string; title: string; hasFile: boolean }>;
  homework: UniversityHomework[];
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

export const getUniversitySchedule = () => apiCall<UniversitySchedule>("/schedule");
export const getUniversityExams = () => apiCall<UniversityExam[]>("/exams");
export const getUniversityNews = () => apiCall<UniversityNews[]>("/news");
export const getUniversityLearning = () => apiCall<UniversityLearning>("/learning");

export const submitUniversityHomework = (
  assignmentId: string,
  response: FormattedText,
) =>
  apiCall<UniversityHomework>(`/learning/homework/${assignmentId}`, {
    method: "PUT",
    body: JSON.stringify({ response }),
  });

export const getUniversityExercise = (assignmentId: string) =>
  apiCall<ExerciseResponse>(`/learning/homework/${assignmentId}`);

export const submitUniversityExercise = (
  assignmentId: string,
  answers: AnswerPayload[],
) =>
  apiCall<ExerciseResult>(`/learning/homework/${assignmentId}/answers`, {
    method: "PUT",
    body: JSON.stringify({ answers }),
  });

export async function downloadUniversityPresentation(
  id: string,
  fileName: string,
): Promise<void> {
  const res = await fetch(`${API_BASE}/learning/presentations/${id}/file`, {
    credentials: "include",
  });
  if (!res.ok) throw (await res.json()) as ApiError;
  const url = URL.createObjectURL(await res.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
