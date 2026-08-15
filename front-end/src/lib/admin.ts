// API client for the teacher-only backoffice (/api/v1/admin/**).
// Mirrors lib/auth.ts: cookie-based auth (credentials: "include"), {error,message} errors.
// Methods are added per feature area (availability, homework, presentations).

import { API_ORIGIN } from "@/lib/api";
import type { FormattedText } from "@/components/learning/richtext/types";
import type {
  ExerciseResult,
  HomeworkComposition,
  HomeworkFormat as LearningHomeworkFormat,
  StudentQuestion,
} from "@/lib/learning";
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
// Extended-class eligibility (grant/revoke 1.5-hour booking access)
// ---------------------------------------------------------------------------

export interface ExtendedClassEligibility {
  id: string;
  extendedClassEligible: boolean;
}

export const grantExtendedClass = (id: string) =>
  apiCall<ExtendedClassEligibility>(`/users/${id}/extended-class`, {
    method: "POST",
  });

export const revokeExtendedClass = (id: string) =>
  apiCall<ExtendedClassEligibility>(`/users/${id}/extended-class`, {
    method: "DELETE",
  });

// Kept separate from getStudents() — that roster is shared by pickers that have no
// need for booking-duration eligibility (homework assignment, presentation sharing).
export const getExtendedClassEligibleStudentIds = () =>
  apiCall<string[]>("/students/extended-class-eligible-ids");

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
  companionStudentId: string | null;
  companionStudentEmail: string | null;
  companionStudentFirstName: string | null;
  companionStudentLastName: string | null;
  companionStudentUsername: string | null;
  companionStudentNoShow: boolean | null;
}

export const getAdminBookings = () => apiCall<AdminBooking[]>("/bookings");

export const createAdminBooking = (body: {
  studentId: string;
  date: string; // YYYY-MM-DD, interpreted in the teacher's timezone
  time: string; // HH:mm
  durationMinutes: number;
}) =>
  apiCall<AdminBooking>("/bookings", {
    method: "POST",
    body: JSON.stringify(body),
  });

export const cancelAdminBooking = (id: string) =>
  apiCall<void>(`/bookings/${id}`, { method: "DELETE" });

export type StudentRole = "BOOKING_STUDENT" | "COMPANION";

export const setBookingNoShow = (
  id: string,
  noShow: boolean,
  studentRole?: StudentRole,
) =>
  apiCall<void>(`/bookings/${id}/no-show`, {
    method: "PUT",
    body: JSON.stringify({ noShow, studentRole }),
  });

export const attachCompanionStudent = (bookingId: string, studentId: string) =>
  apiCall<AdminBooking>(`/bookings/${bookingId}/companion-student`, {
    method: "POST",
    body: JSON.stringify({ studentId }),
  });

export const detachCompanionStudent = (bookingId: string) =>
  apiCall<void>(`/bookings/${bookingId}/companion-student`, {
    method: "DELETE",
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
  submissionId: string | null;
  hasTeacherFeedback: boolean;
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
  isCompanionStudent: boolean;
}

export interface StudentProfileHomework {
  id: string;
  title: string;
  status: string;
  submittedAt: string | null;
  needsReview: boolean;
  submissionId: string | null;
  scorePercent: number | null;
  hasTeacherFeedback: boolean;
}

export interface StudentProfilePresentation {
  id: string;
  title: string;
  level: HomeworkLevel | null;
}

export interface HomeworkBreakdown {
  pending: number;
  submitted: number;
  completed: number;
}

const COMPLETED_HOMEWORK_STATUSES = new Set(["REVIEWED", "GRADED"]);

/** Three-bucket homework status counts for the admin student profile Tareas card. */
export function homeworkBreakdownFromList(
  homeworks: Pick<StudentProfileHomework, "status">[],
): HomeworkBreakdown {
  let pending = 0;
  let submitted = 0;
  let completed = 0;
  for (const hw of homeworks) {
    if (COMPLETED_HOMEWORK_STATUSES.has(hw.status)) {
      completed++;
    } else if (hw.status === "SUBMITTED") {
      submitted++;
    } else {
      pending++;
    }
  }
  return { pending, submitted, completed };
}

export interface StudentProfile {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  username: string | null;
  avatarImageId: string | null;
  createdAt: string;
  interests: string[];
  interestsNote: string | null;
  bookings: StudentProfileBooking[];
  homeworks: StudentProfileHomework[];
  presentations: StudentProfilePresentation[];
}

export const getStudentProfile = (id: string) =>
  apiCall<StudentProfile>(`/students/${id}/profile`);

export type HomeworkType = "AUDIO" | "WRITE" | "GRAMMAR" | "READ";
export type HomeworkLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type HomeworkFormat = LearningHomeworkFormat;
export type { HomeworkComposition };
export type QuestionKind =
  | "SINGLE_CHOICE"
  | "MULTI_CHOICE"
  | "MULTI_BLANK"
  | "DRAG_DROP"
  | "TABLE_FILL"
  | "MATCHING"
  | "TRUE_FALSE"
  | "FREE_TEXT";

export interface AdminOption {
  id?: string;
  label: string;
  correct: boolean;
}

// --- Structured question payloads (authoring / stored, includes answer key) ---
// See specs/024-new-exercise-types/contracts/exercise-types-api.md

export interface MultiBlankStructure {
  blanks: { acceptedAnswers: string[] }[];
}

export interface BankItem {
  id: string;
  label: string;
}

export interface DragDropBlankKey {
  /** Bank item ids that count as correct for this blank (any-of). */
  correctBankIds: string[];
}

export interface DragDropStructure {
  bank: BankItem[];
  /**
   * Canonical answer key: one entry per `___` blank.
   * Legacy payloads omit this — bank[i] was the sole correct item for blank i.
   */
  blanks?: DragDropBlankKey[];
}

export interface TableFillCell {
  r: number;
  c: number;
  type: "fixed" | "blank";
  text?: string; // fixed cells
  acceptedAnswers?: string[]; // blank cells
}

export interface TableFillStructure {
  rowHeaders: string[];
  colHeaders: string[];
  cells: TableFillCell[];
}

export interface MatchingItem {
  id: string;
  label: string;
}

export interface MatchingPair {
  leftId: string;
  rightId: string;
}

export interface MatchingStructure {
  left: MatchingItem[];
  right: MatchingItem[];
  pairs: MatchingPair[];
}

/** Numbered opción única: one pick-one list per `(N)` marker. */
export interface SingleChoiceItemOption {
  id?: string;
  label: string;
  correct: boolean;
}

export interface SingleChoiceItem {
  number: number;
  options: SingleChoiceItemOption[];
}

export interface SingleChoiceStructure {
  items: SingleChoiceItem[];
}

export type QuestionStructure =
  | MultiBlankStructure
  | DragDropStructure
  | TableFillStructure
  | MatchingStructure
  | SingleChoiceStructure
  | Record<string, never>; // legacy kinds: {}

export interface AdminQuestion {
  id?: string;
  kind: QuestionKind;
  prompt: string;
  /** Choice kinds (classic SINGLE_CHOICE / MULTI_CHOICE / TRUE_FALSE); empty when numbered or structured. */
  options: AdminOption[];
  /** Structured kinds and numbered SINGLE_CHOICE items; {} for classic choice / FREE_TEXT. */
  structure?: QuestionStructure;
}

export interface HomeworkAdminItem {
  id: string;
  title: string;
  instructions: string;
  dueOn: string | null;
  homeworkType: HomeworkType | null;
  level: HomeworkLevel | null;
  /** Derived cache; prefer composition for UI. */
  format: HomeworkFormat;
  composition?: HomeworkComposition | null;
  questions: AdminQuestion[];
  audioUrl: string | null; // listening homework external source
  audioFileId: string | null; // listening homework uploaded file
  audioFileName: string | null; // original filename of the uploaded audio
  mediaSourceKind: MediaSourceKind | null;
  assignees: Assignee[];
}

export type MediaSourceKind =
  | "AUDIO_URL"
  | "UPLOADED_FILE"
  | "VIDEO_PAGE"
  | "YOUTUBE";

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
  mediaSourceKind: MediaSourceKind | null;
  audioUrl: string | null;
  audioFileId: string | null;
}

export const createHomework = (
  title: string,
  instructions: string,
  dueOn: string | null,
  homeworkType: HomeworkType | null,
  level: HomeworkLevel | null,
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
      questions,
      audioUrl: audio.audioUrl,
      audioFileId: audio.audioFileId,
      mediaSourceKind: audio.mediaSourceKind,
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
      questions,
      audioUrl: audio.audioUrl,
      audioFileId: audio.audioFileId,
      mediaSourceKind: audio.mediaSourceKind,
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
  composition?: HomeworkComposition | null;
  format?: HomeworkFormat | null;
}

export interface ManualSubmissionAnswerAdmin {
  questionId: string | null;
  promptSnapshot: string;
  text: string;
  formatted?: FormattedText | null;
  kind?: QuestionKind | null;
  teacherScorePercent?: number | null;
  score?: number | null;
  correct?: boolean | null;
}

export type ReviewModel = "LEGACY_RICH" | "ANNOTATED" | null;

export interface HomeworkSubmissionAdmin {
  submissionId: string;
  studentId: string;
  studentEmail: string;
  studentFirstName: string | null;
  studentLastName: string | null;
  studentUsername: string | null;
  assignmentTitle: string;
  status: "PENDING" | "SUBMITTED" | "REVIEWED" | "GRADED";
  composition?: HomeworkComposition | null;
  format?: HomeworkFormat | null;
  reviewModel?: ReviewModel;
  scorePercent?: number | null;
  /** WRITE MANUAL: rich-text answer. Multi MANUAL: null/empty. */
  response: FormattedText | null;
  /** Per-question answers (FREE_TEXT + optional auto metadata for MIXED). */
  answers?: ManualSubmissionAnswerAdmin[] | null;
  /** MIXED / ALL_AUTO: structured questions for read-only auto results. */
  questions?: StudentQuestion[];
  result?: ExerciseResult | null;
  /** LEGACY_RICH rich feedback only. */
  feedback: FormattedText | null;
  /** ANNOTATED plain note (≤500). */
  feedbackText?: string | null;
  /** WRITE: teacher percent (admin always; student only when GRADED). */
  teacherScorePercent?: number | null;
  submittedAt: string | null;
  reviewedAt: string | null;
}

export interface SaveHomeworkReviewPayload {
  feedbackText?: string | null;
  response?: FormattedText | null;
  /** When true, require all percents and set GRADED. Omit/false = progress save. */
  finalize?: boolean;
  /** WRITE: 0–100 teacher percent (required on finalize). */
  teacherScorePercent?: number | null;
  answers?: {
    questionId: string | null;
    formatted: FormattedText;
    /** 0–100; required for every FREE_TEXT on finalize. */
    teacherScorePercent?: number | null;
  }[];
}

export const getHomeworkReviewQueue = () =>
  apiCall<HomeworkReviewQueueItem[]>("/homework/submissions");

export const getHomeworkSubmission = (submissionId: string) =>
  apiCall<HomeworkSubmissionAdmin>(`/homework/submissions/${submissionId}`);

export interface ExerciseSubmissionResultAdmin {
  submissionId: string;
  assignmentId: string;
  assignmentTitle: string;
  studentId: string;
  studentEmail: string;
  studentFirstName: string | null;
  studentLastName: string | null;
  studentUsername: string | null;
  questions: StudentQuestion[];
  result: ExerciseResult;
  teacherFeedback: string | null;
}

export const getExerciseSubmissionResult = (submissionId: string) =>
  apiCall<ExerciseSubmissionResultAdmin>(
    `/homework/submissions/${submissionId}/exercise-result`,
  );

export const saveExerciseFeedback = (submissionId: string, feedback: string) =>
  apiCall<ExerciseSubmissionResultAdmin>(
    `/homework/submissions/${submissionId}/exercise-feedback`,
    {
      method: "PUT",
      body: JSON.stringify({ feedback }),
    },
  );

export const saveHomeworkFeedback = (
  submissionId: string,
  payload: SaveHomeworkReviewPayload,
) =>
  apiCall<HomeworkSubmissionAdmin>(
    `/homework/submissions/${submissionId}/feedback`,
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
  );

// ---------------------------------------------------------------------------
// Presentations (User Story 3)
// ---------------------------------------------------------------------------

export interface PresentationFileSummary {
  id: string;
  displayName: string;
  originalName: string;
  contentType: string;
  byteSize: number;
  createdAt: string;
}

export interface PresentationSummary {
  id: string;
  title: string;
  level: HomeworkLevel | null;
  files: PresentationFileSummary[];
  sharedWithIds: string[];
  updatedAt: string;
}

export interface PresentationDetail {
  id: string;
  title: string;
  level: HomeworkLevel | null;
  files: PresentationFileSummary[];
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
  const res = await fetch(`${API_BASE}/presentations/${id}/files`, {
    method: "POST",
    credentials: "include",
    body: form,
  });
  const data = await res.json();
  if (!res.ok) throw data as ApiError;
  return data as PresentationDetail;
};

export const deletePresentationFile = (id: string, fileId: string) =>
  apiCall<void>(`/presentations/${id}/files/${fileId}`, { method: "DELETE" });

export const downloadPresentationFile = async (
  id: string,
  fileId: string,
  fileName: string,
): Promise<void> => {
  const res = await fetch(`${API_BASE}/presentations/${id}/files/${fileId}`, {
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

// ---------------------------------------------------------------------------
// Presentation Activities
// ---------------------------------------------------------------------------

export interface ActivityInstructionsMeta {
  id: string;
  originalName: string;
  contentType: string;
  byteSize: number;
}

export interface ActivityAdminItem {
  id: string;
  title: string;
  format: HomeworkFormat;
  composition?: HomeworkComposition | null;
  level: HomeworkLevel | null;
  homeworkType: HomeworkType | null;
  presentationId: string;
  presentationTitle: string;
  position: number;
  triggerFileId: string | null;
  triggerPage: number | null;
  instructionsText: string;
  youtubeUrl: string | null;
  imageId: string | null;
  hasInstructions: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ActivityAdminDetail extends ActivityAdminItem {
  questions: AdminQuestion[];
  instructions: ActivityInstructionsMeta | null;
}

export interface ActivityWriteFields {
  title: string;
  presentationId: string;
  /** Ignored by server; format is derived from questions. */
  format?: HomeworkFormat;
  level?: HomeworkLevel | null;
  homeworkType?: HomeworkType | null;
  triggerFileId: string;
  triggerPage: number;
  instructionsText: string;
  youtubeUrl?: string | null;
  imageId?: string | null;
  questions?: AdminQuestion[];
}

export const listActivities = (presentationId?: string) => {
  const qs = presentationId
    ? `?presentationId=${encodeURIComponent(presentationId)}`
    : "";
  return apiCall<ActivityAdminItem[]>(`/activities${qs}`);
};

export const getActivityAdmin = (id: string) =>
  apiCall<ActivityAdminDetail>(`/activities/${id}`);

export const uploadAdminImage = async (
  file: File,
): Promise<{ id: string; contentType: string; byteSize: number }> => {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${API_BASE}/images`, {
    method: "POST",
    credentials: "include",
    body: form,
  });
  const data = await res.json();
  if (!res.ok) throw data as ApiError;
  return data as { id: string; contentType: string; byteSize: number };
};

export const createActivity = (fields: ActivityWriteFields) =>
  apiCall<ActivityAdminDetail>("/activities", {
    method: "POST",
    body: JSON.stringify({
      title: fields.title,
      presentationId: fields.presentationId,
      level: fields.level ?? null,
      homeworkType: fields.homeworkType ?? null,
      triggerFileId: fields.triggerFileId,
      triggerPage: fields.triggerPage,
      instructionsText: fields.instructionsText,
      youtubeUrl: fields.youtubeUrl ?? null,
      imageId: fields.imageId ?? null,
      questions: fields.questions ?? [],
    }),
  });

export const updateActivity = (id: string, fields: ActivityWriteFields) =>
  apiCall<ActivityAdminDetail>(`/activities/${id}`, {
    method: "PUT",
    body: JSON.stringify({
      title: fields.title,
      presentationId: fields.presentationId,
      level: fields.level ?? null,
      homeworkType: fields.homeworkType ?? null,
      triggerFileId: fields.triggerFileId,
      triggerPage: fields.triggerPage,
      instructionsText: fields.instructionsText,
      youtubeUrl: fields.youtubeUrl ?? null,
      imageId: fields.imageId ?? null,
      questions: fields.questions ?? [],
    }),
  });

export const deleteActivity = (id: string) =>
  apiCall<void>(`/activities/${id}`, { method: "DELETE" });

export const reorderActivities = (
  presentationId: string,
  activityIds: string[],
) =>
  apiCall<void>(`/presentations/${presentationId}/activities/reorder`, {
    method: "PUT",
    body: JSON.stringify({ activityIds }),
  });

export const getActivityReviewQueue = () =>
  apiCall<HomeworkReviewQueueItem[]>("/activities/submissions");

export const getActivitySubmission = (submissionId: string) =>
  apiCall<HomeworkSubmissionAdmin>(`/activities/submissions/${submissionId}`);

export const getActivityExerciseSubmissionResult = (submissionId: string) =>
  apiCall<ExerciseSubmissionResultAdmin>(
    `/activities/submissions/${submissionId}/exercise-result`,
  );

export const saveActivityFeedback = (
  submissionId: string,
  payload: SaveHomeworkReviewPayload,
) =>
  apiCall<HomeworkSubmissionAdmin>(
    `/activities/submissions/${submissionId}/feedback`,
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
  );

export const saveActivityExerciseFeedback = (
  submissionId: string,
  feedback: string,
) =>
  apiCall<ExerciseSubmissionResultAdmin>(
    `/activities/submissions/${submissionId}/exercise-feedback`,
    {
      method: "PUT",
      body: JSON.stringify({ feedback }),
    },
  );

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

export interface UnitContentItem {
  type: "PRESENTATION" | "HOMEWORK";
  unitPosition: number;
  presentation: PresentationSummary | null;
  homework: HomeworkAdminItem | null;
}

export interface UnitDetail {
  id: string;
  level: HomeworkLevel;
  subject: string;
  position: number;
  contents: UnitContentItem[];
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

export const reorderUnitContents = (
  id: string,
  items: { type: "PRESENTATION" | "HOMEWORK"; id: string }[],
) =>
  apiCall<UnitDetail>(`/units/${id}/contents/reorder`, {
    method: "PUT",
    body: JSON.stringify({ items }),
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
// Quizzes — /api/v1/admin/quizzes/**
// ---------------------------------------------------------------------------

export type QuizSkill = "READING" | "WRITING" | "GRAMMAR" | "LISTENING";

export interface QuizAdminQuestion extends AdminQuestion {
  skill: QuizSkill;
  mediaSourceKind?: MediaSourceKind | null;
  audioUrl?: string | null;
  audioFileId?: string | null;
}

export interface QuizAdminListItem {
  id: string;
  title: string;
  questionCount: number;
  assigneeCount: number;
  attemptCount: number;
}

export interface QuizAssignee {
  id: string;
  name: string;
  email: string;
}

export interface QuizAdminDetail {
  id: string;
  title: string;
  description: string | null;
  questions: QuizAdminQuestion[];
  assignees: QuizAssignee[];
}

export interface QuizAttemptListItem {
  id: string;
  userId: string;
  studentName: string;
  email: string | null;
  status: string;
  scorePercent: number | null;
  submittedAt: string | null;
}

export interface StudentQuizSummary {
  quizId: string;
  attemptId: string;
  title: string;
  status: string;
  scorePercent: number | null;
  submittedAt: string | null;
  skills: {
    skill: QuizSkill;
    scorePercent: number | null;
    fullyCorrectCount: number | null;
    questionUnitCount: number;
    awaitingTeacher: boolean;
  }[];
}

export const listAdminQuizzes = () =>
  apiCall<QuizAdminListItem[]>("/quizzes");

export const createQuiz = (title: string, description?: string | null) =>
  apiCall<QuizAdminDetail>("/quizzes", {
    method: "POST",
    body: JSON.stringify({ title, description: description ?? null }),
  });

export const getAdminQuiz = (id: string) =>
  apiCall<QuizAdminDetail>(`/quizzes/${id}`);

export const updateQuiz = (
  id: string,
  title: string,
  description: string | null,
  questions: QuizAdminQuestion[],
) =>
  apiCall<QuizAdminDetail>(`/quizzes/${id}`, {
    method: "PUT",
    body: JSON.stringify({ title, description, questions }),
  });

export const deleteQuiz = (id: string) =>
  apiCall<void>(`/quizzes/${id}`, { method: "DELETE" });

export const setQuizAssignees = (id: string, studentIds: string[]) =>
  apiCall<QuizAdminDetail>(`/quizzes/${id}/assignees`, {
    method: "PUT",
    body: JSON.stringify({ studentIds }),
  });

export const listQuizAttempts = (id: string) =>
  apiCall<QuizAttemptListItem[]>(`/quizzes/${id}/attempts`);

export const getQuizAttempt = (quizId: string, attemptId: string) =>
  apiCall<import("@/lib/quiz").QuizTakeResponse>(
    `/quizzes/${quizId}/attempts/${attemptId}`,
  );

export const reviewQuizAttempt = (
  quizId: string,
  attemptId: string,
  body: {
    answers?: { questionId: string; teacherScorePercent?: number | null }[];
    feedbackText?: string | null;
    finalize?: boolean;
  },
) =>
  apiCall<import("@/lib/quiz").QuizTakeResponse>(
    `/quizzes/${quizId}/attempts/${attemptId}/review`,
    { method: "PUT", body: JSON.stringify(body) },
  );

export const getStudentQuizzes = (studentId: string) =>
  apiCall<StudentQuizSummary[]>(`/students/${studentId}/quizzes`);

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
