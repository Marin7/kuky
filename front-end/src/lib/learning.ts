const API_BASE = "http://localhost:8081/api/v1";

export type HomeworkStatus = "PENDING" | "SUBMITTED" | "REVIEWED";

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
  status: HomeworkStatus;
  response: string | null;
  submittedAt: string | null; // ISO instant or null
  overdue: boolean;
}

export interface LearningResponse {
  presentation: PresentationBlock[];
  pastClasses: PastClass[];
  homework: HomeworkItem[];
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

export const getLearning = () => apiCall<LearningResponse>("/learning");

export const submitHomework = (assignmentId: string, response?: string) =>
  apiCall<HomeworkItem>(`/learning/homework/${assignmentId}`, {
    method: "PUT",
    body: JSON.stringify({ response: response ?? null }),
  });
