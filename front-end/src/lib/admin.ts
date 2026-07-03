// API client for the teacher-only backoffice (/api/v1/admin/**).
// Mirrors lib/auth.ts: cookie-based auth (credentials: "include"), {error,message} errors.
// Methods are added per feature area (availability, homework, presentations).

import { API_ORIGIN } from "@/lib/api";
import type { FormattedText } from "@/components/learning/richtext/types";
const API_BASE = `${API_ORIGIN}/api/v1/admin`;

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

export function studentDisplayName(
  s: Pick<Student, "firstName" | "lastName" | "username" | "email">,
): string {
  if (s.firstName && s.lastName) return `${s.firstName} ${s.lastName}`;
  if (s.firstName) return s.firstName;
  if (s.username) return `@${s.username}`;
  return s.email.split("@")[0];
}

export const getStudents = () => apiCall<Student[]>("/students");

// ---------------------------------------------------------------------------
// Registered users (grant/revoke student status)
// ---------------------------------------------------------------------------

export interface RegisteredUser {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  username: string | null;
}

export interface UserRole {
  id: string;
  role: "USER" | "STUDENT" | "ADMIN";
}

export const getRegisteredUsers = () => apiCall<RegisteredUser[]>("/users");

export const promoteToStudent = (id: string) =>
  apiCall<UserRole>(`/users/${id}/student`, { method: "POST" });

export const revokeStudent = (id: string) =>
  apiCall<UserRole>(`/users/${id}/student`, { method: "DELETE" });

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
  slotEnd: string; // ISO
  zoomJoinUrl: string | null;
}

export const getAdminBookings = () => apiCall<AdminBooking[]>("/bookings");

export const cancelAdminBooking = (id: string) =>
  apiCall<void>(`/bookings/${id}`, { method: "DELETE" });

export const setBookingNoShow = (id: string, noShow: boolean) =>
  apiCall<void>(`/bookings/${id}/no-show`, {
    method: "PUT",
    body: JSON.stringify({ noShow }),
  });

// ---------------------------------------------------------------------------
// Availability (User Story 1)
// ---------------------------------------------------------------------------

// The general weekly template (the default week new weeks are seeded from).
export interface WeeklyWindow {
  id?: string;
  dayOfWeek: number; // 1=Mon … 7=Sun
  startTime: string; // "HH:mm"
  endTime: string;
}

// One absolute available window on a concrete date.
export interface DayWindow {
  startTime: string; // "HH:mm"
  endTime: string;
}

// The materialized source-of-truth availability for one date.
export interface DayAvailability {
  date: string; // "YYYY-MM-DD"
  windows: DayWindow[];
}

export interface BookingConflict {
  bookingId: string;
  studentEmail: string;
  slotStart: string; // ISO
}

export interface AvailabilityResponse {
  weekly: WeeklyWindow[];
  days: DayAvailability[];
}

export interface UpdateWeeklyResponse {
  weekly: WeeklyWindow[];
  bookingConflicts: BookingConflict[];
}

export interface UpdateDayResponse {
  date: string;
  windows: DayWindow[];
  bookingConflicts: BookingConflict[];
}

export const getAvailability = () =>
  apiCall<AvailabilityResponse>("/availability");

export const updateWeekly = (windows: WeeklyWindow[]) =>
  apiCall<UpdateWeeklyResponse>("/availability/weekly", {
    method: "PUT",
    body: JSON.stringify({ windows }),
  });

// Replace all windows for a single date (per-week customization).
export const setDayAvailability = (date: string, windows: DayWindow[]) =>
  apiCall<UpdateDayResponse>(`/availability/days/${date}`, {
    method: "PUT",
    body: JSON.stringify({ windows }),
  });

// ---------------------------------------------------------------------------
// Homework (User Story 2)
// ---------------------------------------------------------------------------

export interface Assignee {
  userId: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  username: string | null;
  status: "PENDING" | "SUBMITTED" | "REVIEWED" | "GRADED";
  responseText: string | null;
  submittedAt: string | null;
  scorePercent: number | null;
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
  noShow: boolean;
}

export interface StudentProfileHomework {
  id: string;
  title: string;
  status: string;
  submittedAt: string | null;
  needsReview: boolean;
  submissionId: string | null;
}

export interface StudentProfilePresentation {
  id: string;
  title: string;
  level: HomeworkLevel | null;
}

export interface UnitProgress {
  unitId: string;
  subject: string;
  level: HomeworkLevel;
  totalHomeworks: number;
  completedHomeworks: number;
  complete: boolean;
}

export interface HomeworkBreakdown {
  pending: number;
  submitted: number;
  completed: number;
}

export interface StudentProgress {
  units: UnitProgress[];
  homeworkBreakdown: HomeworkBreakdown;
  attendedClasses: number;
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
  progress: StudentProgress;
}

export const getStudentProfile = (id: string) =>
  apiCall<StudentProfile>(`/students/${id}/profile`);

export type HomeworkType = "AUDIO" | "WRITE" | "GRAMMAR" | "READ";
export type HomeworkLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type HomeworkFormat = "MANUAL" | "EXERCISE";
export type QuestionKind = "SINGLE_CHOICE" | "MULTI_CHOICE" | "FILL_BLANK";

export interface AdminOption {
  id?: string;
  label: string;
  correct: boolean;
}

export interface AdminQuestion {
  id?: string;
  kind: QuestionKind;
  prompt: string;
  options: AdminOption[];
}

export interface HomeworkAdminItem {
  id: string;
  title: string;
  instructions: string;
  dueOn: string | null;
  homeworkType: HomeworkType | null;
  level: HomeworkLevel | null;
  format: HomeworkFormat;
  questions: AdminQuestion[];
  audioUrl: string | null; // listening homework external source
  audioFileId: string | null; // listening homework uploaded file
  audioFileName: string | null; // original filename of the uploaded audio
  assignees: Assignee[];
}

export interface AudioUpload {
  id: string;
  originalName: string;
  contentType: string;
  byteSize: number;
}

export const getHomework = () => apiCall<HomeworkAdminItem[]>("/homework");

export const getHomeworkById = (id: string) =>
  apiCall<HomeworkAdminItem>(`/homework/${id}`);

export interface HomeworkAudio {
  audioUrl: string | null;
  audioFileId: string | null;
}

export const createHomework = (
  title: string,
  instructions: string,
  dueOn: string | null,
  homeworkType: HomeworkType | null,
  level: HomeworkLevel | null,
  format: HomeworkFormat,
  questions: AdminQuestion[],
  audio: HomeworkAudio,
  assigneeIds: string[],
) =>
  apiCall<HomeworkAdminItem>("/homework", {
    method: "POST",
    body: JSON.stringify({
      title,
      instructions,
      dueOn,
      homeworkType,
      level,
      format,
      questions,
      audioUrl: audio.audioUrl,
      audioFileId: audio.audioFileId,
      assigneeIds,
    }),
  });

export const updateHomework = (
  id: string,
  title: string,
  instructions: string,
  dueOn: string | null,
  homeworkType: HomeworkType | null,
  level: HomeworkLevel | null,
  format: HomeworkFormat,
  questions: AdminQuestion[],
  audio: HomeworkAudio,
) =>
  apiCall<HomeworkAdminItem>(`/homework/${id}`, {
    method: "PUT",
    body: JSON.stringify({
      title,
      instructions,
      dueOn,
      homeworkType,
      level,
      format,
      questions,
      audioUrl: audio.audioUrl,
      audioFileId: audio.audioFileId,
    }),
  });

// Audio upload is multipart — does not use the JSON apiCall helper.
export const uploadHomeworkAudio = async (file: File): Promise<AudioUpload> => {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${API_BASE}/audio`, {
    method: "POST",
    credentials: "include",
    body: form,
  });
  const data = await res.json();
  if (!res.ok) throw data as ApiError;
  return data as AudioUpload;
};

export const setAssignees = (id: string, assigneeIds: string[]) =>
  apiCall<HomeworkAdminItem>(`/homework/${id}/assignees`, {
    method: "PUT",
    body: JSON.stringify({ assigneeIds }),
  });

export const deleteHomework = (id: string) =>
  apiCall<void>(`/homework/${id}`, { method: "DELETE" });

// ---------------------------------------------------------------------------
// Teacher review of Writing (MANUAL) submissions
// ---------------------------------------------------------------------------

export interface HomeworkReviewQueueItem {
  submissionId: string;
  studentId: string;
  studentEmail: string;
  studentFirstName: string | null;
  studentLastName: string | null;
  studentUsername: string | null;
  assignmentTitle: string;
  submittedAt: string;
}

export interface HomeworkSubmissionAdmin {
  submissionId: string;
  studentId: string;
  studentEmail: string;
  studentFirstName: string | null;
  studentLastName: string | null;
  studentUsername: string | null;
  assignmentTitle: string;
  status: "PENDING" | "SUBMITTED" | "REVIEWED";
  response: FormattedText;
  feedback: FormattedText | null;
  submittedAt: string | null;
  reviewedAt: string | null;
}

export const getHomeworkReviewQueue = () =>
  apiCall<HomeworkReviewQueueItem[]>("/homework/submissions");

export const getHomeworkSubmission = (submissionId: string) =>
  apiCall<HomeworkSubmissionAdmin>(`/homework/submissions/${submissionId}`);

export const saveHomeworkFeedback = (
  submissionId: string,
  feedback: FormattedText,
) =>
  apiCall<HomeworkSubmissionAdmin>(
    `/homework/submissions/${submissionId}/feedback`,
    {
      method: "PUT",
      body: JSON.stringify({ feedback }),
    },
  );

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

// ---------------------------------------------------------------------------
// Units (Class Packages)
// ---------------------------------------------------------------------------

export interface UnitSummary {
  id: string;
  level: HomeworkLevel;
  subject: string;
  position: number;
  presentationCount: number;
  homeworkCount: number;
  assignedStudentIds: string[];
}

export interface UnitDetail {
  id: string;
  level: HomeworkLevel;
  subject: string;
  position: number;
  presentations: PresentationSummary[];
  homeworks: HomeworkAdminItem[];
  assignedStudents: Student[];
}

export const listUnits = () => apiCall<UnitSummary[]>("/units");

export const createUnit = (level: HomeworkLevel, subject: string) =>
  apiCall<UnitDetail>("/units", {
    method: "POST",
    body: JSON.stringify({ level, subject }),
  });

export const getUnit = (id: string) => apiCall<UnitDetail>(`/units/${id}`);

export const updateUnit = (id: string, level: HomeworkLevel, subject: string) =>
  apiCall<UnitDetail>(`/units/${id}`, {
    method: "PUT",
    body: JSON.stringify({ level, subject }),
  });

export const deleteUnit = (id: string) =>
  apiCall<void>(`/units/${id}`, { method: "DELETE" });

export const reorderUnits = (level: HomeworkLevel, orderedIds: string[]) =>
  apiCall<UnitSummary[]>("/units/reorder", {
    method: "PUT",
    body: JSON.stringify({ level, orderedIds }),
  });

export const setUnitPresentations = (id: string, presentationIds: string[]) =>
  apiCall<UnitDetail>(`/units/${id}/presentations`, {
    method: "PUT",
    body: JSON.stringify({ presentationIds }),
  });

export const setUnitHomeworks = (id: string, homeworkIds: string[]) =>
  apiCall<UnitDetail>(`/units/${id}/homeworks`, {
    method: "PUT",
    body: JSON.stringify({ homeworkIds }),
  });

export const setUnitAssignees = (id: string, studentIds: string[]) =>
  apiCall<UnitDetail>(`/units/${id}/assignees`, {
    method: "PUT",
    body: JSON.stringify({ studentIds }),
  });

// ---------------------------------------------------------------------------
// Placement test authoring (User Story 3) — /api/v1/admin/placement/**
// ---------------------------------------------------------------------------

export type PlacementSkill = "READING" | "LISTENING" | "GRAMMAR";
export type PlacementQuestionKind =
  | "SINGLE_CHOICE"
  | "MULTI_CHOICE"
  | "FILL_BLANK";

export interface PlacementConfig {
  readingTimeSeconds: number;
  listeningTimeSeconds: number;
  grammarTimeSeconds: number;
  writingTimeSeconds: number;
  writingPrompt: string;
}

export type CefrLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";

export interface PlacementLevelThreshold {
  level: CefrLevel;
  minScorePercent: number;
}

export interface AdminPlacementOption {
  id?: string;
  label: string;
  isCorrect: boolean;
}

export interface AdminPlacementQuestion {
  id: string;
  skill: PlacementSkill;
  position: number;
  kind: PlacementQuestionKind;
  prompt: string;
  audioUrl: string | null;
  audioFileId: string | null;
  active: boolean;
  options: AdminPlacementOption[];
}

export interface UpsertPlacementQuestion {
  skill: PlacementSkill;
  kind: PlacementQuestionKind;
  prompt: string;
  audioUrl: string | null;
  audioFileId: string | null;
  active: boolean;
  options: { label: string; isCorrect: boolean }[];
}

export interface PlacementSkillResult {
  skill: PlacementSkill;
  scorePercent: number;
  cefrLevel: string;
}

export interface PlacementWritingSubmission {
  id: string;
  body: string;
  submittedAt: string;
}

export interface StudentPlacementEvaluation {
  result: {
    overallCefr: string | null;
    completedAt: string | null;
    skills: PlacementSkillResult[];
  } | null;
  writing: PlacementWritingSubmission[];
}

export const getPlacementConfig = () =>
  apiCall<PlacementConfig>("/placement/config");

export const updatePlacementConfig = (config: PlacementConfig) =>
  apiCall<PlacementConfig>("/placement/config", {
    method: "PUT",
    body: JSON.stringify(config),
  });

export const getPlacementQuestions = (skill: PlacementSkill) =>
  apiCall<AdminPlacementQuestion[]>(`/placement/questions?skill=${skill}`);

export const createPlacementQuestion = (question: UpsertPlacementQuestion) =>
  apiCall<AdminPlacementQuestion>("/placement/questions", {
    method: "POST",
    body: JSON.stringify(question),
  });

export const updatePlacementQuestion = (
  id: string,
  question: UpsertPlacementQuestion,
) =>
  apiCall<AdminPlacementQuestion>(`/placement/questions/${id}`, {
    method: "PUT",
    body: JSON.stringify(question),
  });

export const deletePlacementQuestion = (id: string) =>
  apiCall<void>(`/placement/questions/${id}`, { method: "DELETE" });

export const reorderPlacementQuestions = (
  skill: PlacementSkill,
  orderedIds: string[],
) =>
  apiCall<void>(`/placement/questions/reorder?skill=${skill}`, {
    method: "PUT",
    body: JSON.stringify({ orderedIds }),
  });

export const getStudentPlacementEvaluation = (studentId: string) =>
  apiCall<StudentPlacementEvaluation>(
    `/placement/students/${studentId}/evaluation`,
  );

export const getPlacementLevelThresholds = () =>
  apiCall<PlacementLevelThreshold[]>("/placement/levels");

export const updatePlacementLevelThresholds = (
  thresholds: PlacementLevelThreshold[],
) =>
  apiCall<PlacementLevelThreshold[]>("/placement/levels", {
    method: "PUT",
    body: JSON.stringify(thresholds),
  });

// ---------------------------------------------------------------------------
// Testimonials review/curation
// ---------------------------------------------------------------------------

export type TestimonialStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "UNPUBLISHED";

export interface AdminTestimonial {
  id: string;
  text: string;
  studentName: string;
  status: TestimonialStatus;
  displayOrder: number;
  submittedAt: string;
  reviewedAt: string | null;
}

export const getAdminTestimonials = () =>
  apiCall<AdminTestimonial[]>("/testimonials");

export const approveTestimonial = (id: string) =>
  apiCall<AdminTestimonial>(`/testimonials/${id}/approve`, { method: "POST" });

export const rejectTestimonial = (id: string) =>
  apiCall<AdminTestimonial>(`/testimonials/${id}/reject`, { method: "POST" });

export const unpublishTestimonial = (id: string) =>
  apiCall<AdminTestimonial>(`/testimonials/${id}/unpublish`, {
    method: "POST",
  });

export const updateTestimonialText = (id: string, text: string) =>
  apiCall<AdminTestimonial>(`/testimonials/${id}`, {
    method: "PUT",
    body: JSON.stringify({ text }),
  });

export const reorderTestimonials = (orderedIds: string[]) =>
  apiCall<AdminTestimonial[]>("/testimonials/reorder", {
    method: "PUT",
    body: JSON.stringify({ orderedIds }),
  });

export const deleteTestimonial = (id: string) =>
  apiCall<void>(`/testimonials/${id}`, { method: "DELETE" });
