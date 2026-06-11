const API_BASE = "http://localhost:8081/api/v1";

export type HomeworkStatus = "PENDING" | "SUBMITTED" | "REVIEWED";
export type HomeworkType = "AUDIO" | "WRITE" | "GRAMMAR" | "READ";
export type HomeworkLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";

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
  status: HomeworkStatus;
  response: string | null;
  submittedAt: string | null; // ISO instant or null
  overdue: boolean;
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

export const getLearning = () => apiCall<LearningResponse>("/learning");

export const submitHomework = (assignmentId: string, response?: string) =>
  apiCall<HomeworkItem>(`/learning/homework/${assignmentId}`, {
    method: "PUT",
    body: JSON.stringify({ response: response ?? null }),
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
