// API client for the teacher-only backoffice (/api/v1/admin/**).
// Mirrors lib/auth.ts: cookie-based auth (credentials: "include"), {error,message} errors.
// Methods are added per feature area (availability, homework, presentations).

const API_BASE = "http://localhost:8081/api/v1/admin";

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

// ---------------------------------------------------------------------------
// Students (shared by homework assignment + presentation sharing)
// ---------------------------------------------------------------------------

export interface Student {
  id: string;
  email: string;
}

export const getStudents = () => apiCall<Student[]>("/students");

// ---------------------------------------------------------------------------
// Availability (User Story 1)
// ---------------------------------------------------------------------------

export interface WeeklyWindow {
  id?: string;
  dayOfWeek: number; // 1=Mon … 7=Sun
  startTime: string; // "HH:mm"
  endTime: string;
}

export interface AvailabilityException {
  id: string;
  date: string; // "YYYY-MM-DD"
  kind: "BLOCK" | "OPEN";
  startTime: string;
  endTime: string;
}

export interface BookingConflict {
  bookingId: string;
  studentEmail: string;
  slotStart: string; // ISO
}

export interface AvailabilityResponse {
  weekly: WeeklyWindow[];
  exceptions: AvailabilityException[];
}

export interface UpdateWeeklyResponse {
  weekly: WeeklyWindow[];
  bookingConflicts: BookingConflict[];
}

export const getAvailability = () =>
  apiCall<AvailabilityResponse>("/availability");

export const updateWeekly = (windows: WeeklyWindow[]) =>
  apiCall<UpdateWeeklyResponse>("/availability/weekly", {
    method: "PUT",
    body: JSON.stringify({ windows }),
  });

export const addException = (
  date: string,
  kind: "BLOCK" | "OPEN",
  startTime: string,
  endTime: string,
) =>
  apiCall<AvailabilityException>("/availability/exceptions", {
    method: "POST",
    body: JSON.stringify({ date, kind, startTime, endTime }),
  });

export const deleteException = (id: string) =>
  apiCall<void>(`/availability/exceptions/${id}`, { method: "DELETE" });

// ---------------------------------------------------------------------------
// Homework (User Story 2)
// ---------------------------------------------------------------------------

export interface Assignee {
  userId: string;
  email: string;
  status: "PENDING" | "SUBMITTED" | "REVIEWED";
  responseText: string | null;
  submittedAt: string | null;
}

export interface HomeworkAdminItem {
  id: string;
  title: string;
  instructions: string;
  dueOn: string | null;
  assignees: Assignee[];
}

export const getHomework = () => apiCall<HomeworkAdminItem[]>("/homework");

export const createHomework = (
  title: string,
  instructions: string,
  dueOn: string | null,
  assigneeIds: string[],
) =>
  apiCall<HomeworkAdminItem>("/homework", {
    method: "POST",
    body: JSON.stringify({ title, instructions, dueOn, assigneeIds }),
  });

export const updateHomework = (
  id: string,
  title: string,
  instructions: string,
  dueOn: string | null,
) =>
  apiCall<HomeworkAdminItem>(`/homework/${id}`, {
    method: "PUT",
    body: JSON.stringify({ title, instructions, dueOn }),
  });

export const setAssignees = (id: string, assigneeIds: string[]) =>
  apiCall<HomeworkAdminItem>(`/homework/${id}/assignees`, {
    method: "PUT",
    body: JSON.stringify({ assigneeIds }),
  });

export const deleteHomework = (id: string) =>
  apiCall<void>(`/homework/${id}`, { method: "DELETE" });

// ---------------------------------------------------------------------------
// Presentations (User Story 3)
// ---------------------------------------------------------------------------

export interface PresentationSummary {
  id: string;
  title: string;
  hasFile: boolean;
  originalFileName: string | null;
  sharedWith: number;
  updatedAt: string;
}

export interface PresentationDetail {
  id: string;
  title: string;
  hasFile: boolean;
  originalFileName: string | null;
  sharedWith: Student[];
}

export const listPresentations = () =>
  apiCall<PresentationSummary[]>("/presentations");

export const createPresentation = (title: string) =>
  apiCall<PresentationDetail>("/presentations", {
    method: "POST",
    body: JSON.stringify({ title }),
  });

export const getPresentation = (id: string) =>
  apiCall<PresentationDetail>(`/presentations/${id}`);

export const renamePresentation = (id: string, title: string) =>
  apiCall<PresentationDetail>(`/presentations/${id}`, {
    method: "PUT",
    body: JSON.stringify({ title }),
  });

export const deletePresentation = (id: string) =>
  apiCall<void>(`/presentations/${id}`, { method: "DELETE" });

export const setShares = (presentationId: string, studentIds: string[]) =>
  apiCall<PresentationDetail>(`/presentations/${presentationId}/shares`, {
    method: "PUT",
    body: JSON.stringify({ studentIds }),
  });

// File upload is multipart — does not use the JSON apiCall helper.
export const uploadPresentationFile = async (
  id: string,
  file: File,
): Promise<PresentationDetail> => {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${API_BASE}/presentations/${id}/file`, {
    method: "POST",
    credentials: "include",
    body: form,
  });
  const data = await res.json();
  if (!res.ok) throw data as ApiError;
  return data as PresentationDetail;
};

export const deletePresentationFile = (id: string) =>
  apiCall<void>(`/presentations/${id}/file`, { method: "DELETE" });
