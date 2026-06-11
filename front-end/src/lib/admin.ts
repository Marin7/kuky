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
  firstName: string | null;
  lastName: string | null;
  username: string | null;
}

export function studentDisplayName(s: Pick<Student, "firstName" | "lastName" | "username" | "email">): string {
  if (s.firstName && s.lastName) return `${s.firstName} ${s.lastName}`;
  if (s.firstName) return s.firstName;
  if (s.username) return `@${s.username}`;
  return s.email.split("@")[0];
}

export const getStudents = () => apiCall<Student[]>("/students");

// ---------------------------------------------------------------------------
// Bookings (admin upcoming view)
// ---------------------------------------------------------------------------

export interface AdminBooking {
  id: string;
  studentId: string;
  studentEmail: string;
  studentFirstName: string | null;
  studentLastName: string | null;
  studentUsername: string | null;
  slotStart: string; // ISO
  slotEnd: string;   // ISO
  zoomJoinUrl: string | null;
}

export const getAdminBookings = () => apiCall<AdminBooking[]>("/bookings");

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
  firstName: string | null;
  lastName: string | null;
  username: string | null;
  status: "PENDING" | "SUBMITTED" | "REVIEWED";
  responseText: string | null;
  submittedAt: string | null;
}

// ---------------------------------------------------------------------------
// Student profiles
// ---------------------------------------------------------------------------

export interface StudentProfileBooking {
  id: string;
  slotStart: string;
  slotEnd: string;
  status: string;
  zoomJoinUrl: string | null;
}

export interface StudentProfileHomework {
  id: string;
  title: string;
  status: string;
  submittedAt: string | null;
}

export interface StudentProfilePresentation {
  id: string;
  title: string;
  level: HomeworkLevel | null;
}

export interface StudentProfile {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  username: string | null;
  avatarImageId: string | null;
  createdAt: string;
  bookings: StudentProfileBooking[];
  homeworks: StudentProfileHomework[];
  presentations: StudentProfilePresentation[];
}

export const getStudentProfile = (id: string) =>
  apiCall<StudentProfile>(`/students/${id}/profile`);

export type HomeworkType = "AUDIO" | "WRITE" | "GRAMMAR" | "READ";
export type HomeworkLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";

export interface HomeworkAdminItem {
  id: string;
  title: string;
  instructions: string;
  dueOn: string | null;
  homeworkType: HomeworkType | null;
  level: HomeworkLevel | null;
  assignees: Assignee[];
}

export const getHomework = () => apiCall<HomeworkAdminItem[]>("/homework");

export const createHomework = (
  title: string,
  instructions: string,
  dueOn: string | null,
  homeworkType: HomeworkType | null,
  level: HomeworkLevel | null,
  assigneeIds: string[],
) =>
  apiCall<HomeworkAdminItem>("/homework", {
    method: "POST",
    body: JSON.stringify({ title, instructions, dueOn, homeworkType, level, assigneeIds }),
  });

export const updateHomework = (
  id: string,
  title: string,
  instructions: string,
  dueOn: string | null,
  homeworkType: HomeworkType | null,
  level: HomeworkLevel | null,
) =>
  apiCall<HomeworkAdminItem>(`/homework/${id}`, {
    method: "PUT",
    body: JSON.stringify({ title, instructions, dueOn, homeworkType, level }),
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
  level: HomeworkLevel | null;
  hasFile: boolean;
  originalFileName: string | null;
  sharedWithIds: string[];
  updatedAt: string;
}

export interface PresentationDetail {
  id: string;
  title: string;
  level: HomeworkLevel | null;
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

export const setPresentationLevel = (id: string, level: HomeworkLevel | null) =>
  apiCall<PresentationDetail>(`/presentations/${id}/level`, {
    method: "PUT",
    body: JSON.stringify({ level }),
  });

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
